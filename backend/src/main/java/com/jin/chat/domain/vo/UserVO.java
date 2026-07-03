package com.jin.chat.domain.vo;

import lombok.Data;

/**
 * 用户视图对象。
 *
 * @author jinshuai
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String role;

    private String status;
}
