package com.jin.chat.domain.vo;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 消息视图对象。
 *
 * @author jinshuai
 */
@Data
public class MessageVO {

    private Long id;

    private Long roomId;

    private Long senderId;

    private String senderName;

    private String content;

    private String type;

    private String status;

    private OffsetDateTime submittedAt;

    private OffsetDateTime reviewedAt;
}
