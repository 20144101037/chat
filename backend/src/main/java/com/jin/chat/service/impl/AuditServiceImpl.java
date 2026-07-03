package com.jin.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jin.chat.common.api.PageResult;
import com.jin.chat.common.constant.RedisKeyConst;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.common.exception.ErrorCodeEnum;
import com.jin.chat.common.lock.DistributedLock;
import com.jin.chat.common.util.TransactionUtils;
import com.jin.chat.domain.ao.BatchAuditAO;
import com.jin.chat.domain.entity.AuditLogDO;
import com.jin.chat.domain.entity.MessageDO;
import com.jin.chat.domain.enums.AuditActionEnum;
import com.jin.chat.domain.enums.MessageStatusEnum;
import com.jin.chat.domain.enums.MessageTypeEnum;
import com.jin.chat.domain.dto.WsMessage;
import com.jin.chat.domain.vo.MessageVO;
import com.jin.chat.mapper.AuditLogMapper;
import com.jin.chat.mapper.MessageMapper;
import com.jin.chat.repository.AuditQueueRepository;
import com.jin.chat.service.AuditService;
import com.jin.chat.service.MessageService;
import com.jin.chat.service.MonitorService;
import com.jin.chat.service.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * <p>
 * 审核服务实现。并发与一致性保障：
 * <ul>
 *     <li>分布式锁（DistributedLock）：多实例下对同一消息串行化，减少无效竞争；</li>
 *     <li>数据库 CAS（updateStatusCas）：状态更新原子性，affected=0 表示已被并发处理；</li>
 *     <li>@Transactional：审核状态更新与审核日志写入保证事务性；</li>
 *     <li>afterCommit 推送：仅在事务提交成功后推送，避免回滚产生“幽灵消息”。</li>
 * </ul>
 * </p>
 *
 * @author jinshuai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    /** 审核分布式锁 TTL */
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);

    private final MessageMapper messageMapper;
    private final AuditLogMapper auditLogMapper;
    private final MessageService messageService;
    private final PushService pushService;
    private final AuditQueueRepository auditQueueRepository;
    private final DistributedLock distributedLock;
    private final MonitorService monitorService;

    @Override
    public PageResult<MessageVO> listPending(Long roomId, long pageNo, long pageSize) {
        LambdaQueryWrapper<MessageDO> wrapper = new LambdaQueryWrapper<MessageDO>()
                .eq(MessageDO::getStatus, MessageStatusEnum.PENDING_REVIEW.name())
                .orderByAsc(MessageDO::getSubmittedAt);
        if (roomId != null) {
            wrapper.eq(MessageDO::getRoomId, roomId);
        }
        Page<MessageDO> page = messageService.page(new Page<>(pageNo, pageSize), wrapper);
        return PageResult.from(page, messageService::toVO);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void approve(Long messageId, Long reviewerId) {
        String key = RedisKeyConst.AUDIT_LOCK_PREFIX + messageId;
        String token = UUID.randomUUID().toString();
        if (!distributedLock.tryLock(key, token, LOCK_TTL)) {
            throw new BusinessException(ErrorCodeEnum.AUDIT_CONCURRENT_CONFLICT);
        }
        try {
            MessageDO message = requirePending(messageId);
            OffsetDateTime now = OffsetDateTime.now();

            int affected = messageMapper.updateStatusCas(
                    messageId, MessageStatusEnum.APPROVED.name(), reviewerId, now);
            if (affected == 0) {
                throw new BusinessException(ErrorCodeEnum.MESSAGE_ALREADY_REVIEWED);
            }
            writeAuditLog(messageId, reviewerId, AuditActionEnum.APPROVE.name(), null);
            auditQueueRepository.remove(messageId);
            recordLatency(message.getSubmittedAt(), now);

            // 回填状态用于推送；事务提交后再推送，避免回滚产生幽灵消息
            message.setStatus(MessageStatusEnum.APPROVED.name());
            message.setReviewedAt(now);
            Long roomId = message.getRoomId();
            WsMessage ws = messageService.toWsMessage(message, MessageTypeEnum.CHAT.name());
            TransactionUtils.afterCommit(() -> pushService.pushToRoom(roomId, ws));
        } finally {
            distributedLock.unlock(key, token);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void reject(Long messageId, Long reviewerId, String reason) {
        String key = RedisKeyConst.AUDIT_LOCK_PREFIX + messageId;
        String token = UUID.randomUUID().toString();
        if (!distributedLock.tryLock(key, token, LOCK_TTL)) {
            throw new BusinessException(ErrorCodeEnum.AUDIT_CONCURRENT_CONFLICT);
        }
        try {
            MessageDO message = requirePending(messageId);
            OffsetDateTime now = OffsetDateTime.now();

            int affected = messageMapper.updateStatusCas(
                    messageId, MessageStatusEnum.REJECTED.name(), reviewerId, now);
            if (affected == 0) {
                throw new BusinessException(ErrorCodeEnum.MESSAGE_ALREADY_REVIEWED);
            }
            writeAuditLog(messageId, reviewerId, AuditActionEnum.REJECT.name(), reason);
            auditQueueRepository.remove(messageId);
            recordLatency(message.getSubmittedAt(), now);

            Long senderId = message.getSenderId();
            WsMessage ws = WsMessage.builder()
                    .type(MessageTypeEnum.NOTIFICATION.name())
                    .roomId(message.getRoomId())
                    .messageId(messageId)
                    .content("您的消息未通过审核" + (reason == null ? "" : "：" + reason))
                    .status(MessageStatusEnum.REJECTED.name())
                    .timestamp(now)
                    .build();
            TransactionUtils.afterCommit(() -> pushService.notifyUser(senderId, ws));
        } finally {
            distributedLock.unlock(key, token);
        }
    }

    @Override
    public void batchAudit(BatchAuditAO ao, Long reviewerId) {
        boolean approve = AuditActionEnum.APPROVE.name().equals(ao.getAction());
        for (Long messageId : ao.getMessageIds()) {
            try {
                if (approve) {
                    approve(messageId, reviewerId);
                } else {
                    reject(messageId, reviewerId, ao.getReason());
                }
            } catch (BusinessException e) {
                // 单条失败不影响其余，记录后继续
                log.warn("批量审核跳过 messageId={}, reason={}", messageId, e.getMessage());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void handleTimeout(Long messageId) {
        String key = RedisKeyConst.AUDIT_LOCK_PREFIX + messageId;
        String token = UUID.randomUUID().toString();
        if (!distributedLock.tryLock(key, token, LOCK_TTL)) {
            // 正被管理员审核，跳过本次超时处理
            return;
        }
        try {
            MessageDO message = messageMapper.selectById(messageId);
            if (message == null || !MessageStatusEnum.PENDING_REVIEW.name().equals(message.getStatus())) {
                auditQueueRepository.remove(messageId);
                return;
            }
            OffsetDateTime now = OffsetDateTime.now();
            int affected = messageMapper.updateStatusCas(
                    messageId, MessageStatusEnum.TIMEOUT.name(), null, now);
            auditQueueRepository.remove(messageId);
            if (affected == 0) {
                // 已被管理员抢先审核，无需处理
                return;
            }
            writeAuditLog(messageId, null, AuditActionEnum.TIMEOUT.name(), "审核超时自动拒绝");
            recordLatency(message.getSubmittedAt(), now);

            Long senderId = message.getSenderId();
            WsMessage ws = WsMessage.builder()
                    .type(MessageTypeEnum.NOTIFICATION.name())
                    .roomId(message.getRoomId())
                    .messageId(messageId)
                    .content("您的消息因审核超时未能发布")
                    .status(MessageStatusEnum.TIMEOUT.name())
                    .timestamp(now)
                    .build();
            TransactionUtils.afterCommit(() -> pushService.notifyUser(senderId, ws));
        } finally {
            distributedLock.unlock(key, token);
        }
    }

    /**
     * 记录审核处理延迟（提交 -> 审核完成）。
     */
    private void recordLatency(OffsetDateTime submittedAt, OffsetDateTime reviewedAt) {
        if (submittedAt == null || reviewedAt == null) {
            return;
        }
        monitorService.recordAuditLatency(Duration.between(submittedAt, reviewedAt));
    }

    private MessageDO requirePending(Long messageId) {
        MessageDO message = messageMapper.selectById(messageId);
        if (Objects.isNull(message)) {
            throw new BusinessException(ErrorCodeEnum.MESSAGE_NOT_EXIST);
        }
        if (!MessageStatusEnum.PENDING_REVIEW.name().equals(message.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.MESSAGE_ALREADY_REVIEWED);
        }
        return message;
    }

    private void writeAuditLog(Long messageId, Long reviewerId, String action, String reason) {
        AuditLogDO log = new AuditLogDO();
        log.setMessageId(messageId);
        log.setReviewerId(reviewerId);
        log.setAction(action);
        log.setReason(reason);
        auditLogMapper.insert(log);
    }
}
