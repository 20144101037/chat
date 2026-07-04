package com.jin.chat.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 用户-角色关联。纯关联表，重新分配时物理删除后重建，
 * 故不使用逻辑删除（避免与唯一键冲突）。
 * </p>
 *
 * @author jinshuai
 */
@Data
@TableName("user_roles")
public class UserRoleDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long roleId;
}
