package com.jin.chat.domain.ao;

import lombok.Data;

/**
 * 单条审核入参。
 *
 * @author jinshuai
 */
@Data
public class AuditAO {

    /** 拒绝时的原因（可选） */
    private String reason;
}
