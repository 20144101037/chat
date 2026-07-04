package com.jin.chat.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 业务错误码枚举。区间约定：
 * <ul>
 *     <li>0：成功</li>
 *     <li>1000+：通用/系统</li>
 *     <li>2000+：认证与用户</li>
 *     <li>3000+：聊天室与成员</li>
 *     <li>4000+：消息与审核</li>
 * </ul>
 * </p>
 *
 * @author jinshuai
 */
@Getter
@AllArgsConstructor
public enum ErrorCodeEnum {

    SUCCESS(0, "success"),

    SYSTEM_ERROR(1000, "系统异常，请稍后重试"),
    PARAM_INVALID(1001, "参数校验失败"),
    FORBIDDEN(1002, "无权限执行该操作"),

    ACCOUNT_NOT_LOGIN(2000, "用户未登录或登录已过期"),
    ACCOUNT_NOT_EXIST(2001, "账号不存在"),
    PASSWORD_ERROR(2002, "账号或密码错误"),
    USERNAME_EXIST(2003, "用户名已存在"),
    TOKEN_INVALID(2004, "无效的令牌"),

    ROOM_NOT_EXIST(3000, "聊天室不存在"),
    ROOM_CLOSED(3001, "聊天室已关闭"),
    ROOM_PAUSED(3002, "聊天室已暂停"),
    ROOM_FULL(3003, "聊天室人数已满"),
    NOT_ROOM_MEMBER(3004, "您尚未加入该聊天室"),
    ALREADY_JOINED(3005, "您已加入该聊天室"),
    JOIN_PENDING(3006, "加入申请待管理员审批"),
    CANNOT_KICK_OWNER(3007, "不能移除聊天室创建者"),
    MEMBER_NOT_IN_ROOM(3008, "该用户不在聊天室成员列表中"),

    MESSAGE_NOT_EXIST(4000, "消息不存在"),
    MESSAGE_ALREADY_REVIEWED(4001, "消息已被审核，请勿重复操作"),
    MESSAGE_CONTENT_TOO_LONG(4003, "消息内容超过长度限制"),
    AUDIT_CONCURRENT_CONFLICT(4002, "审核状态并发冲突，请重试"),

    CONFIG_KEY_EXIST(5000, "配置键已存在"),
    CONFIG_NOT_EXIST(5001, "配置项不存在"),
    CONFIG_NOT_EDITABLE(5002, "该配置项不允许修改"),
    CONFIG_VALUE_INVALID(5003, "配置值不合法"),

    ROLE_CODE_EXIST(6000, "角色编码已存在"),
    ROLE_NOT_EXIST(6001, "角色不存在"),
    ROLE_BUILT_IN(6002, "内置角色不可删除或修改编码"),
    MENU_KEY_EXIST(6003, "菜单标识已存在"),
    MENU_NOT_EXIST(6004, "菜单不存在"),
    MENU_HAS_CHILDREN(6005, "请先删除子菜单");

    private final int code;
    private final String message;
}
