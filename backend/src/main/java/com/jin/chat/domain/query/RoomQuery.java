package com.jin.chat.domain.query;

import lombok.Data;

/**
 * 聊天室列表查询过滤条件。
 *
 * @author jinshuai
 */
@Data
public class RoomQuery {

    /** 名称模糊查询 */
    private String keyword;

    /** ACTIVE / PAUSED / CLOSED */
    private String status;

    private long pageNo = 1;

    private long pageSize = 10;
}
