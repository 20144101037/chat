package com.jin.chat.domain.ao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 管理员广播消息入参（可向多个聊天室广播，绕过审核）。
 *
 * @author jinshuai
 */
@Data
public class BroadcastAO {

    @NotEmpty(message = "目标聊天室不能为空")
    private List<Long> roomIds;

    @NotBlank(message = "消息内容不能为空")
    private String content;
}
