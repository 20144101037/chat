package com.jin.chat.ws;

import com.jin.chat.common.context.LoginUser;
import com.jin.chat.common.util.JwtUtil;
import com.jin.chat.repository.TokenValidityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * <p>
 * WebSocket 握手认证拦截器：从查询参数 token 校验 JWT，通过后将登录用户写入握手属性。
 * 握手失败则拒绝建立连接。
 * </p>
 *
 * @author jinshuai
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    public static final String ATTR_LOGIN_USER = "loginUser";

    private final JwtUtil jwtUtil;
    private final TokenValidityRepository tokenValidityRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        String token = servletRequest.getServletRequest().getParameter("token");
        LoginUser user = token == null ? null : jwtUtil.parseToken(token);
        if (user == null || !tokenValidityRepository.isValid(user.getUserId(), user.getIssuedAt())) {
            log.warn("WebSocket 握手认证失败，拒绝连接");
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(ATTR_LOGIN_USER, user);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
