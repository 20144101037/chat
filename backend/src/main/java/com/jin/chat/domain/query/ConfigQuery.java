package com.jin.chat.domain.query;

import lombok.Data;

/**
 * 全局配置列表查询条件。
 *
 * @author jinshuai
 */
@Data
public class ConfigQuery {

    /** 配置键模糊查询 */
    private String keyword;

    /** 分组过滤 */
    private String configGroup;

    private long pageNo = 1;

    private long pageSize = 10;
}
