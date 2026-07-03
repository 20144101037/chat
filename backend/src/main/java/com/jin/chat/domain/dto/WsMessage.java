package com.jin.chat.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * <p>
 * WebSocket 统一消息协议（JSON 信封）。
 * </p>
 *
 * @author jinshuai
 */
@Data
@Builder
public class WsMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** CHAT / NOTIFICATION / SYSTEM / HEARTBEAT / ACK */
    private String type;

    private Long roomId;

    private Long messageId;

    private Long senderId;

    private String senderName;

    private String content;

    /** 消息状态，如 APPROVED */
    private String status;

    private OffsetDateTime timestamp;
}
