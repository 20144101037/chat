package com.jin.chat.service;

import com.jin.chat.common.api.PageResult;
import com.jin.chat.domain.ao.BatchAuditAO;
import com.jin.chat.domain.vo.MessageVO;

/**
 * <p>
 * 审核服务：待审核列表、通过/拒绝、批量审核、超时处理。
 * 无状态设计，支持水平扩展。
 * </p>
 *
 * @author jinshuai
 */
public interface AuditService {

    /**
     * 分页查询待审核消息（可按聊天室筛选）。
     */
    PageResult<MessageVO> listPending(Long roomId, long pageNo, long pageSize);

    /**
     * 审核通过：CAS 更新状态 -> 记录审计 -> 推送到房间 -> 出队。
     */
    void approve(Long messageId, Long reviewerId);

    /**
     * 审核拒绝：CAS 更新状态 -> 记录审计 -> 通知提交者 -> 出队。
     */
    void reject(Long messageId, Long reviewerId, String reason);

    /**
     * 批量审核。
     */
    void batchAudit(BatchAuditAO ao, Long reviewerId);

    /**
     * 超时处理：由定时任务调用，将超时消息标记为 TIMEOUT 并通知提交者。
     */
    void handleTimeout(Long messageId);
}
