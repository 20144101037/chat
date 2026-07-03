package com.jin.chat.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * <p>
 * 用户-聊天室关联表。
 * </p>
 *
 * @author jinshuai
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_chat_rooms")
public class UserChatRoomDO extends BaseDO {

    private Long userId;

    private Long roomId;

    /** PENDING / JOINED / LEFT / REJECTED */
    private String memberStatus;

    /** MEMBER / ADMIN */
    private String roleInRoom;

    private OffsetDateTime joinedAt;
}
