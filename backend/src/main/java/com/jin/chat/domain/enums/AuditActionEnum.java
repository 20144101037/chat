package com.jin.chat.domain.enums;

/**
 * 审核动作。
 *
 * @author jinshuai
 */
public enum AuditActionEnum {
    /** 通过 */
    APPROVE,
    /** 拒绝 */
    REJECT,
    /** 系统超时自动拒绝 */
    TIMEOUT
}
