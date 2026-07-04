package com.jin.chat.repository;

import com.jin.chat.common.constant.RedisKeyConst;
import com.jin.chat.common.constant.SysConfigKeys;
import com.jin.chat.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 已登录用户统计：登录成功写入 Redis，退出/重置密码删除，有效请求续期 TTL（与 JWT 有效期一致）。
 * </p>
 *
 * @author jinshuai
 */
@Repository
@RequiredArgsConstructor
public class LoggedInRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SysConfigService sysConfigService;

    /** 标记用户已登录（登录成功时调用） */
    public void markLoggedIn(Long userId) {
        redisTemplate.opsForValue().set(key(userId), "1", ttlMinutes(), TimeUnit.MINUTES);
    }

    /** 续期登录态 TTL（鉴权通过的 REST / WebSocket 握手时调用） */
    public void refreshLoggedIn(Long userId) {
        String redisKey = key(userId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            redisTemplate.expire(redisKey, ttlMinutes(), TimeUnit.MINUTES);
        } else {
            markLoggedIn(userId);
        }
    }

    /** 清除登录态（主动退出、重置密码/强制下线时调用） */
    public void markLoggedOut(Long userId) {
        redisTemplate.delete(key(userId));
    }

    /** 当前已登录用户数 */
    public long loggedInCount() {
        Set<String> keys = redisTemplate.keys(RedisKeyConst.LOGGED_IN_PREFIX + "*");
        return keys == null ? 0 : keys.size();
    }

    private String key(Long userId) {
        return RedisKeyConst.LOGGED_IN_PREFIX + userId;
    }

    private long ttlMinutes() {
        return sysConfigService.getInt(SysConfigKeys.JWT_EXPIRE_MINUTES, SysConfigKeys.DEFAULT_JWT_EXPIRE_MINUTES);
    }
}
