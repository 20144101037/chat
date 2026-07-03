package com.jin.chat.domain.vo;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 聊天室成员视图对象。
 *
 * @author chat
 */
@Data
public class MemberVO {

    private Long userId;

    private String username;

    private String nickname;

    /** PENDING / JOINED / LEFT / REJECTED */
    private String memberStatus;

    /** MEMBER / ADMIN */
    private String roleInRoom;

    private OffsetDateTime joinedAt;
}
