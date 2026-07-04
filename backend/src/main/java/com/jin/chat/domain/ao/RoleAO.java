package com.jin.chat.domain.ao;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 角色新增/修改入参。
 *
 * @author jinshuai
 */
@Data
public class RoleAO {

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    private String description;
}
