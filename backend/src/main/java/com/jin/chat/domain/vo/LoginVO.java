package com.jin.chat.domain.vo;

import lombok.Data;

/**
 * 登录结果。
 *
 * @author jinshuai
 */
@Data
public class LoginVO {

    private String token;

    private UserVO user;
}
