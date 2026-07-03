package com.jin.chat.service;

import com.jin.chat.domain.dto.WsMessage;

/**
 * <p>
 * WebSocket 推送服务：房间广播、个人通知、待审核推送。
 * </p>
 *
 * @author jinshuai
 */
public interface PushService {

    /**
     * 向指定房间的所有在线订阅者广播消息。
     */
    void pushToRoom(Long roomId, WsMessage message);

    /**
     * 向指定用户发送个人通知（审核结果、超时、加入审批等）。
     */
    void notifyUser(Long userId, WsMessage message);

    /**
     * 向管理端推送新的待审核提醒。
     */
    void pushPendingToAdmins(WsMessage message);
}
