package com.jin.chat.domain.ao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 全局配置修改入参（仅允许修改配置值）。
 *
 * @author jinshuai
 */
@Data
public class ConfigUpdateAO {

    @NotBlank(message = "配置值不能为空")
    @Size(max = 1000, message = "配置值长度不能超过 1000 个字符")
    private String configValue;
}
