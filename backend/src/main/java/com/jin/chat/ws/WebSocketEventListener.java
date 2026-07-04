package com.jin.chat.ws;

import com.jin.chat.common.context.LoginUser;
import com.jin.chat.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

/**
 * <p>
 * WebSocket 会话事件监听：维护在线会话与订阅关系，断线时清理。
 * </p>
 *
 * @author jinshuai
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private static final String ROOM_TOPIC_PREFIX = "/topic/room.";

    private final SessionRepository sessionRepository;

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        // 在线状态已在 StompChannelInterceptor CONNECT 中写入，此处仅作兜底
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = currentUserId(accessor);
        if (userId != null) {
            sessionRepository.refreshOnline(userId);
            log.debug("会话已连接 userId={}, sessionId={}", userId, accessor.getSessionId());
        }
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = currentUserId(accessor);
        String destination = accessor.getDestination();
        if (userId != null && destination != null && destination.startsWith(ROOM_TOPIC_PREFIX)) {
            Long roomId = Long.valueOf(destination.substring(ROOM_TOPIC_PREFIX.length()));
            sessionRepository.subscribeRoom(userId, roomId);
        }
    }

    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        // 生产可结合订阅 ID 映射精确移除房间订阅，这里从略
        log.debug("取消订阅 sessionId={}", StompHeaderAccessor.wrap(event.getMessage()).getSessionId());
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        // 下线已在 StompChannelInterceptor DISCONNECT 中处理，此处兜底刷新订阅清理
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = currentUserId(accessor);
        if (userId != null) {
            log.debug("会话已断开 userId={}, sessionId={}", userId, accessor.getSessionId());
        }
    }

    private Long currentUserId(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof StompPrincipal principal) {
            return principal.getLoginUser().getUserId();
        }
        if (accessor.getSessionAttributes() != null) {
            LoginUser user = (LoginUser) accessor.getSessionAttributes().get(WebSocketAuthInterceptor.ATTR_LOGIN_USER);
            return user != null ? user.getUserId() : null;
        }
        return null;
    }
}
