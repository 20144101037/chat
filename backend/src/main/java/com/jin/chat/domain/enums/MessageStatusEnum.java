package com.jin.chat.domain.enums;

/**
 * 消息审核状态。
 *
 * @author jinshuai
 */
public enum MessageStatusEnum {
    /** 待审核 */
    PENDING_REVIEW,
    /** 审核通过 */
    APPROVED,
    /** 审核拒绝 */
    REJECTED,
    /** 审核超时（自动拒绝） */
    TIMEOUT
}
