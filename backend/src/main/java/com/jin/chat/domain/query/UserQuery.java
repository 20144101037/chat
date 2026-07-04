package com.jin.chat.domain.query;

import lombok.Data;

/**
 * 用户管理列表查询条件。
 *
 * @author jinshuai
 */
@Data
public class UserQuery {

    /** 用户名/昵称模糊查询 */
    private String keyword;

    /** ACTIVE / BANNED */
    private String status;

    private long pageNo = 1;

    private long pageSize = 10;
}
