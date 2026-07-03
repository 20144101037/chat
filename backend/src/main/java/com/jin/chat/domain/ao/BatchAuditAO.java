package com.jin.chat.domain.ao;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量审核入参。
 *
 * @author jinshuai
 */
@Data
public class BatchAuditAO {

    @NotEmpty(message = "待审核消息不能为空")
    private List<Long> messageIds;

    /** APPROVE / REJECT */
    private String action;

    private String reason;
}
