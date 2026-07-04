package com.jin.chat.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 用户管理视图对象（含所属角色）。
 *
 * @author jinshuai
 */
@Data
public class AdminUserVO {

    private Long id;

    private String username;

    private String nickname;

    /** 主角色（最高权限，用于粗粒度鉴权与 JWT） */
    private String role;

    private String status;

    /** 所属角色 ID 列表 */
    private List<Long> roleIds;

    /** 所属角色名称列表（展示用） */
    private List<String> roleNames;
}
