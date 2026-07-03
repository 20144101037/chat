package com.jin.chat.service;

import com.jin.chat.domain.ao.LoginAO;
import com.jin.chat.domain.ao.RegisterAO;
import com.jin.chat.domain.vo.LoginVO;
import com.jin.chat.domain.vo.UserVO;

/**
 * <p>
 * 认证服务：注册、登录。
 * </p>
 *
 * @author jinshuai
 */
public interface AuthService {

    /**
     * 用户注册。
     */
    UserVO register(RegisterAO ao);

    /**
     * 用户登录，返回 JWT 令牌与用户信息。
     */
    LoginVO login(LoginAO ao);
}
