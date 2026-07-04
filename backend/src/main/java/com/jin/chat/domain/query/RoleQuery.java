package com.jin.chat.domain.query;

import lombok.Data;

/**
 * 角色列表查询条件。
 *
 * @author jinshuai
 */
@Data
public class RoleQuery {

    /** 角色编码/名称模糊查询 */
    private String keyword;

    private long pageNo = 1;

    private long pageSize = 10;
}
