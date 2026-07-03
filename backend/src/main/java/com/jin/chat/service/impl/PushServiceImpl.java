package com.jin.chat.service.impl;

import com.jin.chat.domain.dto.WsMessage;
import com.jin.chat.service.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 推送服务实现。基于 SimpMessagingTemplate 走 STOMP Broker 推送。
 * 采用异步执行，实现审核与推送解耦、削峰。
 * </p>
 *
 * <p>at-least-once 语义：推送侧带有限次重试尽量不丢；接收侧由客户端按 messageId 去重，
 * 断线重连后再通过历史接口补齐，二者配合避免消息丢失。</p>
 *
 * <p>注：多实例部署时，可在此接入 Redis Pub/Sub 将消息广播到所有实例后再各自本地推送。</p>
 *
 * @author jinshuai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushServiceImpl implements PushService {

    private static final String ROOM_TOPIC_PREFIX = "/topic/room.";
    private static final String AUDIT_TOPIC = "/topic/audit";
    private static final String USER_NOTIFY_QUEUE = "/queue/notifications";

    /** 推送失败最大重试次数（at-least-once） */
    private static final int MAX_RETRY = 2;

    private final SimpMessagingTemplate messagingTemplate;

    @Async("pushExecutor")
    @Override
    public void pushToRoom(Long roomId, WsMessage message) {
        sendWithRetry(() -> messagingTemplate.convertAndSend(ROOM_TOPIC_PREFIX + roomId, message),
                "房间消息推送 roomId=" + roomId + ", messageId=" + message.getMessageId());
    }

    @Async("pushExecutor")
    @Override
    public void notifyUser(Long userId, WsMessage message) {
        sendWithRetry(() -> messagingTemplate.convertAndSendToUser(String.valueOf(userId), USER_NOTIFY_QUEUE, message),
                "个人通知推送 userId=" + userId);
    }

    @Async("pushExecutor")
    @Override
    public void pushPendingToAdmins(WsMessage message) {
        sendWithRetry(() -> messagingTemplate.convertAndSend(AUDIT_TOPIC, message),
                "待审核推送 messageId=" + message.getMessageId());
    }

    /**
     * 带有限次重试的推送，尽量保证送达（at-least-once）。
     */
    private void sendWithRetry(Runnable sendAction, String desc) {
        int attempt = 0;
        while (true) {
            try {
                sendAction.run();
                return;
            } catch (Exception e) {
                attempt++;
                if (attempt > MAX_RETRY) {
                    log.error("{} 推送失败，已达最大重试次数 {}", desc, MAX_RETRY, e);
                    return;
                }
                log.warn("{} 推送失败，第 {} 次重试", desc, attempt);
            }
        }
    }
}
