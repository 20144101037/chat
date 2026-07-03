package com.jin.chat.ws;

import com.jin.chat.common.context.LoginUser;
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

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            LoginUser user = accessor.getSessionAttributes() == null ? null
                    : (LoginUser) accessor.getSessionAttributes().get(WebSocketAuthInterceptor.ATTR_LOGIN_USER);
            if (user != null) {
                accessor.setUser(new StompPrincipal(user));
            }
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            validateSubscribe(accessor);
        }
        return message;
    }

    private void validateSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || !(accessor.getUser() instanceof StompPrincipal principal)) {
            throw new IllegalArgumentException("未认证的订阅请求");
        }
        LoginUser user = principal.getLoginUser();

        if (destination.startsWith(ROOM_TOPIC_PREFIX)) {
            Long roomId = parseRoomId(destination);
            if (!memberService.isJoined(user.getUserId(), roomId)) {
                throw new IllegalArgumentException("无权订阅该聊天室: " + roomId);
            }
        } else if (destination.equals(AUDIT_TOPIC)
                && !menuPermissionService.hasMenuPath(user.getUserId(), MENU_AUDIT)) {
            throw new IllegalArgumentException("无权订阅审核通道");
        }
    }

    private Long parseRoomId(String destination) {
        try {
            return Long.valueOf(destination.substring(ROOM_TOPIC_PREFIX.length()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("非法的房间订阅地址: " + destination);
        }
    }
}
