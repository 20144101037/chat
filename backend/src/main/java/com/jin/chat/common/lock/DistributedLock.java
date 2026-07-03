package com.jin.chat.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;

/**
 * <p>
 * 基于 Redis 的轻量分布式锁（SET NX PX + Lua 原子释放）。
 * 用于多实例部署下对同一资源（如同一条消息的审核）串行化，
 * 与数据库 CAS 乐观锁配合，确保状态更新的原子性。
 * </p>
 *
 * @author jinshuai
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLock {

    /** 释放锁的原子脚本：仅当 value 等于自己的 token 时才删除，避免误删他人锁 */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 尝试加锁（非阻塞）。
     *
     * @param key   锁 key
     * @param token 持有者标识（释放时校验）
     * @param ttl   锁自动过期时间，避免持有者宕机导致死锁
     * @return 是否加锁成功
     */
    public boolean tryLock(String key, String token, Duration ttl) {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
        return Boolean.TRUE.equals(ok);
    }

    /**
     * 释放锁（仅释放自己持有的）。
     */
    public void unlock(String key, String token) {
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
        } catch (Exception e) {
            log.warn("释放分布式锁失败 key={}", key, e);
        }
    }
}
