package com.jin.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jin.chat.common.api.PageResult;
import com.jin.chat.domain.ao.ConfigUpdateAO;
import com.jin.chat.domain.entity.SysConfigDO;
import com.jin.chat.domain.query.ConfigQuery;
import com.jin.chat.domain.vo.ConfigRuntimeVO;

/**
 * <p>
 * 全局配置服务：分页查询、仅修改配置值（不支持新增/删除），以及供业务读取配置值（带本地缓存）。
 * </p>
 *
 * @author jinshuai
 */
public interface SysConfigService extends IService<SysConfigDO> {

    PageResult<SysConfigDO> page(ConfigQuery query);

    /**
     * 修改配置值（仅允许修改 value，且需通过合法性校验）。
     */
    SysConfigDO update(Long id, ConfigUpdateAO ao);

    /**
     * 读取字符串配置，不存在时返回默认值。
     */
    String getString(String key, String defaultValue);

    /**
     * 读取整型配置，不存在或解析失败时返回默认值。
     */
    int getInt(String key, int defaultValue);

    /** 供前端表单默认值与运行时校验使用的配置快照 */
    ConfigRuntimeVO getRuntime();
}
