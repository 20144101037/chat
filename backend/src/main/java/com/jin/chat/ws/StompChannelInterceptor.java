package com.jin.chat.ws;

import com.jin.chat.common.context.LoginUser;
import com.jin.chat.repository.SessionRepository;
import com.jin.chat.service.MemberService;
import com.jin.chat.service.MenuPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

/**
 * <p>
 * STOMP 通道拦截器：
 * <ul>
 *     <li>CONNECT：将握手阶段的登录用户绑定为连接 Principal。</li>
 *     <li>SUBSCRIBE：校验订阅权限——房间需为已加入成员，/topic/audit 需管理员。</li>
 * </ul>
 * </p>
 *
 * @author jinshuai
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private static final String ROOM_TOPIC_PREFIX = "/topic/room.";
    private static final String AUDIT_TOPIC = "/topic/audit";
    private static final String MENU_AUDIT = "/app/audit";

    @Lazy
    @Autowired
    private MemberService memberService;

    @Lazy
    @Autowired
    private MenuPermissionService menuPermissionService;

    @Lazy
    @Autowired
    private SessionRepository sessionRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            LoginUser user = resolveLoginUser(accessor);
            if (user != null) {
                accessor.setUser(new StompPrincipal(user));
                // SessionConnectedEvent 中 Principal 可能尚未就绪，在此记录在线状态更可靠
                sessionRepository.online(user.getUserId(), accessor.getSessionId());
                log.info("STOMP 连接上线 userId={}, sessionId={}", user.getUserId(), accessor.getSessionId());
            }
        } else if (StompCommand.DISCONNECT.equals(command)) {
            LoginUser user = resolveLoginUser(accessor);
            if (user != null) {
                sessionRepository.offline(user.getUserId());
                log.info("STOMP 断开下线 userId={}, sessionId={}", user.getUserId(), accessor.getSessionId());
            }
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            LoginUser user = resolveLoginUser(accessor);
            if (user != null) {
                sessionRepository.refreshOnline(user.getUserId());
                ensurePrincipal(accessor, user);
            }
            validateSubscribe(accessor);
        }
        return message;
    }

    /** SUBSCRIBE 帧上 Principal 可能未携带，需从 session 补全供后续链路使用 */
    private void ensurePrincipal(StompHeaderAccessor accessor, LoginUser user) {
        if (!(accessor.getUser() instanceof StompPrincipal)) {
            accessor.setUser(new StompPrincipal(user));
        }
    }

    private LoginUser resolveLoginUser(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof StompPrincipal principal) {
            return principal.getLoginUser();
        }
        if (accessor.getSessionAttributes() != null) {
            return (LoginUser) accessor.getSessionAttributes().get(WebSocketAuthInterceptor.ATTR_LOGIN_USER);
        }
        return null;
    }

    /**
     * 仅对敏感 topic 鉴权；个人通知 /user/queue/* 及 Spring 内部解析后的 /queue/* 不做拦截，
     * 避免 getUser() 未就绪时误杀连接。
     */
    private void validateSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }
        if (destination.startsWith(ROOM_TOPIC_PREFIX)) {
            LoginUser user = requireUser(accessor, destination);
            Long roomId = parseRoomId(destination);
            if (!memberService.isJoined(user.getUserId(), roomId)) {
                log.warn("无权订阅聊天室 userId={}, roomId={}", user.getUserId(), roomId);
                throw new IllegalArgumentException("无权订阅该聊天室: " + roomId);
            }
        } else if (destination.equals(AUDIT_TOPIC)) {
            LoginUser user = requireUser(accessor, destination);
            if (!menuPermissionService.hasMenuPath(user.getUserId(), MENU_AUDIT)) {
                log.warn("无权订阅审核通道 userId={}", user.getUserId());
                throw new IllegalArgumentException("无权订阅审核通道");
            }
        }
    }

    private LoginUser requireUser(StompHeaderAccessor accessor, String destination) {
        LoginUser user = resolveLoginUser(accessor);
        if (user == null) {
            log.warn("订阅被拒绝：无法解析用户 destination={}", destination);
            throw new IllegalArgumentException("未认证的订阅请求");
        }
        ensurePrincipal(accessor, user);
        return user;
    }

    private Long parseRoomId(String destination) {
        try {
            return Long.valueOf(destination.substring(ROOM_TOPIC_PREFIX.length()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("非法的房间订阅地址: " + destination);
        }
    }
}
