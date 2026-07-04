package com.jin.chat.common.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jin.chat.common.constant.RedisKeyConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 二级缓存：L1 本地（Guava）+ L2 分布式（Redis）。
 * <ul>
 *     <li>读取：L1 -> L2 -> 回源（loader），并逐级回填；Guava {@code get(key, Callable)} 提供“单飞”加载，
 *         避免高并发下同一 key 的缓存击穿（同一时刻大量用户登录仅回源一次）。</li>
 *     <li>失效：删除 L2，并通过 Redis Pub/Sub 广播，令所有实例清除各自的 L1，保证多实例一致性。</li>
 * </ul>
 * L1 设置较短的兜底过期时间，即使广播丢失，陈旧数据也会在有限时间内自动收敛。
 * </p>
 *
 * @author jinshuai
 */
@Slf4j
@Component
public class TwoLevelCache implements MessageListener {

    /** L1 空值占位（仅存在于本地缓存，避免缓存穿透；不写入 Redis） */
    private static final Object NULL_HOLDER = new Object();

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    /** L1 本地缓存：容量上限 + 写后过期兜底 */
    private final Cache<String, Object> l1 = CacheBuilder.newBuilder()
            .maximumSize(20_000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public TwoLevelCache(RedisTemplate<String, Object> redisTemplate, StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 读取缓存，未命中时通过 loader 回源并回填 L2/L1。
     *
     * @param key      缓存键
     * @param redisTtl L2 过期时间
     * @param loader   回源逻辑（允许返回 null）
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Duration redisTtl, Callable<T> loader) {
        Object value;
        try {
            value = l1.get(key, () -> loadToL1(key, redisTtl, loader));
        } catch (Exception e) {
            log.warn("二级缓存读取失败，降级直接回源，key={}, err={}", key, e.getMessage());
            return callQuietly(loader);
        }
        return value == NULL_HOLDER ? null : (T) value;
    }

    private <T> Object loadToL1(String key, Duration redisTtl, Callable<T> loader) throws Exception {
        Object fromRedis = redisTemplate.opsForValue().get(key);
        if (fromRedis != null) {
            return fromRedis;
        }
        T loaded = loader.call();
        if (loaded == null) {
            // 仅在本地短暂缓存空值，防穿透；不落 Redis 以免污染共享缓存
            return NULL_HOLDER;
        }
        redisTemplate.opsForValue().set(key, loaded, redisTtl);
        return loaded;
    }

    private <T> T callQuietly(Callable<T> loader) {
        try {
            return loader.call();
        } catch (Exception ex) {
            throw new IllegalStateException("缓存回源执行失败", ex);
        }
    }

    /**
     * 失效单个 key：删除 L2 并广播清除各实例 L1。
     */
    public void evict(String key) {
        redisTemplate.delete(key);
        publish(key);
    }

    /**
     * 按前缀失效：SCAN 删除 L2 匹配键，并广播（payload 以 * 结尾表示前缀）。
     */
    public void evictByPrefix(String prefix) {
        try {
            ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(500).build();
            List<String> keys = new ArrayList<>();
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("按前缀清理 Redis 缓存失败，prefix={}, err={}", prefix, e.getMessage());
        }
        publish(prefix + "*");
    }

    private void publish(String payload) {
        try {
            stringRedisTemplate.convertAndSend(RedisKeyConst.CACHE_INVALIDATE_CHANNEL, payload);
        } catch (Exception e) {
            log.warn("发布缓存失效消息失败，payload={}, err={}", payload, e.getMessage());
            // 广播失败时至少清除本地 L1，保证本实例一致
            handleInvalidation(payload);
        }
    }

    /**
     * Redis Pub/Sub 回调：收到失效广播后清除本实例 L1。
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        handleInvalidation(new String(message.getBody(), StandardCharsets.UTF_8));
    }

    private void handleInvalidation(String payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        if (payload.endsWith("*")) {
            String prefix = payload.substring(0, payload.length() - 1);
            l1.asMap().keySet().removeIf(k -> k.startsWith(prefix));
        } else {
            l1.invalidate(payload);
        }
    }
}
