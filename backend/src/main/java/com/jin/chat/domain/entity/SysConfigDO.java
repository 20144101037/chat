package com.jin.chat.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 全局配置项。
 *
 * @author jinshuai
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_config")
public class SysConfigDO extends BaseDO {

    /** 配置键（唯一） */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 配置分组，便于分类展示 */
    private String configGroup;

    /** 描述 */
    private String description;

    /** 是否允许在界面编辑 */
    private Boolean editable;
}
