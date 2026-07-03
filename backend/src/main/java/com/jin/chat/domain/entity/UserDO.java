package com.jin.chat.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 用户表。
 * </p>
 *
 * @author jinshuai
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class UserDO extends BaseDO {

    private String username;

    private String passwordHash;

    private String nickname;

    /** USER / ROOM_ADMIN / SYS_ADMIN */
    private String role;

    /** ACTIVE / BANNED */
    private String status;
}
