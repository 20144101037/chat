package com.jin.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jin.chat.common.api.PageResult;
import com.jin.chat.common.constant.SysConfigKeys;
import com.jin.chat.common.context.UserContextHolder;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.common.exception.ErrorCodeEnum;
import com.jin.chat.domain.ao.BroadcastAO;
import com.jin.chat.domain.ao.MessageSubmitAO;
import com.jin.chat.domain.dto.WsMessage;
import com.jin.chat.domain.entity.ChatRoomDO;
import com.jin.chat.domain.entity.MessageDO;
import com.jin.chat.domain.entity.UserDO;
import com.jin.chat.domain.enums.MessageStatusEnum;
import com.jin.chat.domain.enums.MessageTypeEnum;
import com.jin.chat.domain.enums.RoomStatusEnum;
import com.jin.chat.domain.vo.MessageVO;
import com.jin.chat.mapper.MessageMapper;
import com.jin.chat.mapper.UserMapper;
import com.jin.chat.repository.AuditQueueRepository;
import com.jin.chat.service.ChatRoomService;
import com.jin.chat.service.MemberService;
import com.jin.chat.service.MessageService;
import com.jin.chat.service.PushService;
import com.jin.chat.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * 消息服务实现。
 * </p>
 *
 * @author jinshuai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, MessageDO> implements MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final ChatRoomService chatRoomService;
    private final MemberService memberService;
    private final PushService pushService;
    private final AuditQueueRepository auditQueueRepository;
    private final SysConfigService sysConfigService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public MessageVO submit(Long roomId, MessageSubmitAO ao) {
        Long userId = UserContextHolder.currentUserId();
        ChatRoomDO room = chatRoomService.getAvailableRoom(roomId);
        if (RoomStatusEnum.PAUSED.name().equals(room.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.ROOM_PAUSED);
        }
        memberService.assertJoined(userId, roomId);

        int maxLength = sysConfigService.getInt(SysConfigKeys.MESSAGE_MAX_LENGTH, SysConfigKeys.DEFAULT_MESSAGE_MAX_LENGTH);
        if (ao.getContent() != null && ao.getContent().length() > maxLength) {
            throw new BusinessException(ErrorCodeEnum.MESSAGE_CONTENT_TOO_LONG,
                    String.format("消息内容不能超过 %d 个字符", maxLength));
        }

        OffsetDateTime now = OffsetDateTime.now();
        MessageDO message = new MessageDO();
        message.setRoomId(roomId);
        message.setSenderId(userId);
        message.setContent(ao.getContent());
        message.setType(MessageTypeEnum.CHAT.name());
        message.setStatus(MessageStatusEnum.PENDING_REVIEW.name());
        message.setSubmittedAt(now);
        message.setVersion(0);
        save(message);

        // 事务提交后再入队/推送，避免脏读；此处简化为同事务内，生产可用 AFTER_COMMIT 事件
        auditQueueRepository.enqueue(message.getId(), now.toInstant().toEpochMilli());
        pushService.pushPendingToAdmins(toWsMessage(message, MessageTypeEnum.CHAT.name()));

        return toVO(message);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void broadcast(BroadcastAO ao) {
        Long adminId = UserContextHolder.currentUserId();
        OffsetDateTime now = OffsetDateTime.now();
        for (Long roomId : ao.getRoomIds()) {
            ChatRoomDO room = chatRoomService.getAvailableRoom(roomId);
            MessageDO message = new MessageDO();
            message.setRoomId(roomId);
            message.setSenderId(adminId);
            message.setContent(ao.getContent());
            message.setType(MessageTypeEnum.NOTIFICATION.name());
            message.setStatus(MessageStatusEnum.APPROVED.name());
            message.setSubmittedAt(now);
            message.setReviewedAt(now);
            message.setReviewerId(adminId);
            message.setVersion(0);
            save(message);
            pushService.pushToRoom(roomId, toWsMessage(message, MessageTypeEnum.NOTIFICATION.name()));
        }
    }

    @Override
    public void systemNotify(Long roomId, String content) {
        BroadcastAO ao = new BroadcastAO();
        ao.setRoomIds(List.of(roomId));
        ao.setContent(content);
        broadcast(ao);
    }

    @Override
    public List<MessageVO> listApprovedHistory(Long roomId, Long beforeId, int size) {
        List<MessageDO> list = messageMapper.listApprovedByRoom(roomId, beforeId, Math.min(size, 100));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public PageResult<MessageVO> pageMyMessages(Long userId, String status, long pageNo, long pageSize) {
        LambdaQueryWrapper<MessageDO> wrapper = new LambdaQueryWrapper<MessageDO>()
                .eq(MessageDO::getSenderId, userId)
                .orderByDesc(MessageDO::getSubmittedAt);
        if (status != null && !status.isBlank()) {
            wrapper.eq(MessageDO::getStatus, status);
        }
        Page<MessageDO> page = page(new Page<>(pageNo, pageSize), wrapper);
        return PageResult.from(page, this::toVO);
    }

    @Override
    public MessageVO toVO(MessageDO message) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setRoomId(message.getRoomId());
        vo.setSenderId(message.getSenderId());
        vo.setSenderName(resolveSenderName(message.getSenderId()));
        vo.setContent(message.getContent());
        vo.setType(message.getType());
        vo.setStatus(message.getStatus());
        vo.setSubmittedAt(message.getSubmittedAt());
        vo.setReviewedAt(message.getReviewedAt());
        return vo;
    }

    @Override
    public WsMessage toWsMessage(MessageDO message, String type) {
        return WsMessage.builder()
                .type(type)
                .roomId(message.getRoomId())
                .messageId(message.getId())
                .senderId(message.getSenderId())
                .senderName(resolveSenderName(message.getSenderId()))
                .content(message.getContent())
                .status(message.getStatus())
                .timestamp(Optional.ofNullable(message.getSubmittedAt())
                        .orElse(OffsetDateTime.now(ZoneOffset.UTC)))
                .build();
    }

    private String resolveSenderName(Long senderId) {
        if (Objects.isNull(senderId)) {
            return "系统";
        }
        UserDO user = userMapper.selectById(senderId);
        if (user == null) {
            return "未知用户";
        }
        return Optional.ofNullable(user.getNickname()).orElse(user.getUsername());
    }
}
