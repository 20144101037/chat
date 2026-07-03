package com.jin.chat.domain.enums;

/**
 * 消息类型。
 *
 * @author jinshuai
 */
public enum MessageTypeEnum {
    /** 普通聊天消息 */
    CHAT,
    /** 系统通知/管理员广播（绕过审核） */
    NOTIFICATION
}
