package com.jin.chat.domain.ao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户提交消息入参。
 *
 * @author jinshuai
 */
@Data
public class MessageSubmitAO {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容过长")
    private String content;
}
