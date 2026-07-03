package com.jin.chat.domain.enums;

/**
 * 用户-聊天室成员状态。
 *
 * @author jinshuai
 */
public enum MemberStatusEnum {
    /** 待批准（APPROVAL 策略下） */
    PENDING,
    /** 已加入 */
    JOINED,
    /** 已退出 */
    LEFT,
    /** 加入申请被拒绝 */
    REJECTED
}
