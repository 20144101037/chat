package com.jin.chat.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色。
 *
 * @author jinshuai
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("roles")
public class RoleDO extends BaseDO {

    private String roleCode;

    private String roleName;

    private String description;

    /** 内置角色不可删除 */
    private Boolean builtIn;
}
