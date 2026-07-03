package com.jin.chat.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 聊天室表。
 * </p>
 *
 * @author jinshuai
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_rooms")
public class ChatRoomDO extends BaseDO {

    private String name;

    private String description;

    private Integer maxUsers;

    /** OPEN / APPROVAL */
    private String joinPolicy;

    /** ACTIVE / PAUSED / CLOSED */
    private String status;

    /** 创建者/聊天室管理员 */
    private Long ownerId;
}
