package com.jin.chat.ws;

import com.jin.chat.common.context.LoginUser;
import lombok.Getter;

import java.security.Principal;

/**
 * <p>
 * STOMP 连接身份，承载登录用户信息，用于点对点推送 /user/queue/*。
 * </p>
 *
 * @author jinshuai
 */
@Getter
public class StompPrincipal implements Principal {

    private final LoginUser loginUser;

    public StompPrincipal(LoginUser loginUser) {
        this.loginUser = loginUser;
    }

    @Override
    public String getName() {
        return String.valueOf(loginUser.getUserId());
    }
}
