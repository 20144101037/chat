package com.jin.chat.ws;

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
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = currentUserId(accessor);
        if (userId != null) {
            sessionRepository.online(userId, accessor.getSessionId());
            log.info("用户上线 userId={}, sessionId={}", userId, accessor.getSessionId());
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
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Long userId = currentUserId(accessor);
        if (userId != null) {
            sessionRepository.offline(userId);
            log.info("用户下线 userId={}, sessionId={}", userId, accessor.getSessionId());
        }
    }

    private Long currentUserId(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof StompPrincipal principal) {
            return principal.getLoginUser().getUserId();
        }
        return null;
    }
}
