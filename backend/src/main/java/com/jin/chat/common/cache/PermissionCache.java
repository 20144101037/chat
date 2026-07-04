package com.jin.chat.common.cache;

import com.jin.chat.common.constant.RedisKeyConst;
import com.jin.chat.common.util.TransactionUtils;
import com.jin.chat.domain.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * <p>
 * 用户权限缓存（角色列表 / 可见菜单树），基于 {@link TwoLevelCache}。
 * <ul>
 *     <li>读多写少，且登录高峰会瞬时并发拉取，故用二级缓存 + 单飞加载抗并发。</li>
 *     <li>写操作（分配角色/菜单、菜单变更）统一在事务提交后失效，避免脏数据回填。</li>
 * </ul>
 * </p>
 *
 * @author jinshuai
 */
@Component
@RequiredArgsConstructor
public class PermissionCache {

    /** L2 过期时间：兜底防止长期不一致，正常一致性依赖失效广播 */
    private static final Duration TTL = Duration.ofMinutes(30);

    private final TwoLevelCache cache;

    public List<Long> getRoleIds(Long userId, Callable<List<Long>> loader) {
        return cache.get(RedisKeyConst.PERM_ROLE_IDS_PREFIX + userId, TTL, loader);
    }

    public List<MenuVO> getMenus(Long userId, Callable<List<MenuVO>> loader) {
        return cache.get(RedisKeyConst.PERM_MENUS_PREFIX + userId, TTL, loader);
    }

    /** 失效单个用户的角色与菜单缓存（事务提交后执行）。 */
    public void evictUser(Long userId) {
        TransactionUtils.afterCommit(() -> {
            cache.evict(RedisKeyConst.PERM_ROLE_IDS_PREFIX + userId);
            cache.evict(RedisKeyConst.PERM_MENUS_PREFIX + userId);
        });
    }

    /** 失效所有用户的权限缓存（角色-菜单授权或菜单结构变更时，事务提交后执行）。 */
    public void evictAll() {
        TransactionUtils.afterCommit(() -> {
            cache.evictByPrefix(RedisKeyConst.PERM_ROLE_IDS_PREFIX);
            cache.evictByPrefix(RedisKeyConst.PERM_MENUS_PREFIX);
        });
    }
}
