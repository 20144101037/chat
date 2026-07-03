package com.jin.chat.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 审核日志表：审计追踪，一条消息可有多条记录。
 * </p>
 *
 * @author jinshuai
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit_logs")
public class AuditLogDO extends BaseDO {

    private Long messageId;

    /** 审核人；系统自动超时时为 null */
    private Long reviewerId;

    /** APPROVE / REJECT / TIMEOUT */
    private String action;

    private String reason;
}
