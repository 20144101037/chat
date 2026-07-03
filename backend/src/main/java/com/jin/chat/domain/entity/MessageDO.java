package com.jin.chat.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * <p>
 * 消息表。审核状态更新依赖 version 乐观锁保证并发原子性。
 * </p>
 *
 * @author jinshuai
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("messages")
public class MessageDO extends BaseDO {

    private Long roomId;

    private Long senderId;

    private String content;

    /** CHAT / NOTIFICATION */
    private String type;

    /** PENDING_REVIEW / APPROVED / REJECTED / TIMEOUT */
    private String status;

    /** 提交时间：推送排序依据 */
    private OffsetDateTime submittedAt;

    private OffsetDateTime reviewedAt;

    private Long reviewerId;

    @Version
    private Integer version;
}
