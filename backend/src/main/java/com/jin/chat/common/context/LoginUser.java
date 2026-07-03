package com.jin.chat.common.context;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 登录用户上下文对象。
 * </p>
 *
 * @author jinshuai
 */
@Data
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    /** USER / ROOM_ADMIN / SYS_ADMIN */
    private String role;
    /** 令牌签发时间（epoch millis），用于校验令牌是否早于强制失效点 */
    private Long issuedAt;

    public boolean isAdmin() {
        return "ROOM_ADMIN".equals(role) || "SYS_ADMIN".equals(role);
    }

    public boolean isSysAdmin() {
        return "SYS_ADMIN".equals(role);
    }
}
