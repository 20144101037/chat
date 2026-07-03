package com.jin.chat.common.interceptor;

import com.jin.chat.common.context.LoginUser;
import com.jin.chat.common.context.UserContextHolder;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.common.exception.ErrorCodeEnum;
import com.jin.chat.common.util.JwtUtil;
import com.jin.chat.repository.TokenValidityRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * <p>
 * REST 接口 JWT 认证拦截器：校验 Authorization 头并写入线程上下文。
 * </p>
 *
 * @author jinshuai
 */
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final TokenValidityRepository tokenValidityRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader(HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(PREFIX)) {
            throw new BusinessException(ErrorCodeEnum.ACCOUNT_NOT_LOGIN);
        }
        String token = header.substring(PREFIX.length());
        LoginUser user = jwtUtil.parseToken(token);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.TOKEN_INVALID);
        }
        if (!tokenValidityRepository.isValid(user.getUserId(), user.getIssuedAt())) {
            throw new BusinessException(ErrorCodeEnum.TOKEN_INVALID);
        }
        UserContextHolder.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContextHolder.clear();
    }
}
