package com.jin.chat.domain.vo;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 聊天室视图对象。
 *
 * @author jinshuai
 */
@Data
public class RoomVO {

    private Long id;

    private String name;

    private String description;

    private Integer maxUsers;

    private String joinPolicy;

    private String status;

    private Long ownerId;

    /** 当前成员数 */
    private Long memberCount;

    /** 当前登录用户的成员状态：JOINED / PENDING / null（未加入） */
    private String myMemberStatus;

    private OffsetDateTime createdAt;
}
