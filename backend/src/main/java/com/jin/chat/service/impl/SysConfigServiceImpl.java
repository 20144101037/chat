package com.jin.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jin.chat.common.api.PageResult;
import com.jin.chat.common.cache.TwoLevelCache;
import com.jin.chat.common.constant.RedisKeyConst;
import com.jin.chat.common.constant.SysConfigKeys;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.common.exception.ErrorCodeEnum;
import com.jin.chat.common.util.TransactionUtils;
import com.jin.chat.domain.ao.ConfigUpdateAO;
import com.jin.chat.domain.entity.SysConfigDO;
import com.jin.chat.domain.query.ConfigQuery;
import com.jin.chat.domain.vo.ConfigRuntimeVO;
import com.jin.chat.mapper.SysConfigMapper;
import com.jin.chat.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;

/**
 * <p>
 * 全局配置服务实现。读取带本地缓存，写操作后失效缓存。
 * </p>
 *
 * @author jinshuai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfigDO> implements SysConfigService {

    /** 配置值缓存过期时间 */
    private static final Duration CONFIG_TTL = Duration.ofMinutes(30);

    private final TwoLevelCache cache;

    /** 数值型配置的取值范围约束：configKey -> [min, max]（闭区间），用于合法性校验 */
    private static final Map<String, int[]> NUMERIC_RANGES = Map.of(
            SysConfigKeys.JWT_EXPIRE_MINUTES, new int[]{1, 43200},
            SysConfigKeys.AUDIT_MAX_WAIT_SECONDS, new int[]{5, 3600},
            SysConfigKeys.AUDIT_SCAN_INTERVAL_MS, new int[]{1000, 600000},
            SysConfigKeys.MESSAGE_MAX_LENGTH, new int[]{1, 10000},
            SysConfigKeys.ROOM_DEFAULT_MAX_USERS, new int[]{1, 100000},
            SysConfigKeys.PUSH_RETRY_TIMES, new int[]{0, 10}
    );

    @Override
    public PageResult<SysConfigDO> page(ConfigQuery query) {
        LambdaQueryWrapper<SysConfigDO> wrapper = new LambdaQueryWrapper<SysConfigDO>()
                .like(StringUtils.hasText(query.getKeyword()), SysConfigDO::getConfigKey, query.getKeyword())
                .eq(StringUtils.hasText(query.getConfigGroup()), SysConfigDO::getConfigGroup, query.getConfigGroup())
                .orderByAsc(SysConfigDO::getConfigGroup)
                .orderByAsc(SysConfigDO::getConfigKey);
        Page<SysConfigDO> page = page(new Page<>(query.getPageNo(), query.getPageSize()), wrapper);
        return PageResult.from(page, c -> c);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public SysConfigDO update(Long id, ConfigUpdateAO ao) {
        SysConfigDO config = getById(id);
        if (config == null) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_NOT_EXIST);
        }
        if (Boolean.FALSE.equals(config.getEditable())) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_NOT_EDITABLE);
        }
        String value = ao.getConfigValue() == null ? null : ao.getConfigValue().trim();
        validateValue(config.getConfigKey(), value);
        config.setConfigValue(value);
        updateById(config);
        // 提交后失效并广播，保证多实例一致
        String key = config.getConfigKey();
        TransactionUtils.afterCommit(() -> cache.evict(RedisKeyConst.CONFIG_VALUE_PREFIX + key));
        return config;
    }

    /**
     * 配置值合法性校验：数值型配置需为整数且落在允许区间内。
     */
    private void validateValue(String key, String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCodeEnum.CONFIG_VALUE_INVALID, "配置值不能为空");
        }
        int[] range = NUMERIC_RANGES.get(key);
        if (range != null) {
            int num;
            try {
                num = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCodeEnum.CONFIG_VALUE_INVALID,
                        String.format("配置项 [%s] 必须为整数", key));
            }
            if (num < range[0] || num > range[1]) {
                throw new BusinessException(ErrorCodeEnum.CONFIG_VALUE_INVALID,
                        String.format("配置项 [%s] 取值需在 %d ~ %d 之间", key, range[0], range[1]));
            }
        }
    }

    @Override
    public String getString(String key, String defaultValue) {
        // 高频读取（如登录时读取令牌有效期），走二级缓存 + 单飞加载
        String value = cache.get(RedisKeyConst.CONFIG_VALUE_PREFIX + key, CONFIG_TTL, () -> {
            SysConfigDO config = getByKey(key);
            return config == null ? null : config.getConfigValue();
        });
        return value != null ? value : defaultValue;
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("配置项 {} 值 {} 无法解析为整数，使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public ConfigRuntimeVO getRuntime() {
        ConfigRuntimeVO vo = new ConfigRuntimeVO();
        vo.setRoomDefaultMaxUsers(getInt(SysConfigKeys.ROOM_DEFAULT_MAX_USERS, SysConfigKeys.DEFAULT_ROOM_MAX_USERS));
        vo.setMessageMaxLength(getInt(SysConfigKeys.MESSAGE_MAX_LENGTH, SysConfigKeys.DEFAULT_MESSAGE_MAX_LENGTH));
        return vo;
    }

    private SysConfigDO getByKey(String key) {
        return getOne(new LambdaQueryWrapper<SysConfigDO>()
                .eq(SysConfigDO::getConfigKey, key)
                .last("limit 1"), false);
    }
}
