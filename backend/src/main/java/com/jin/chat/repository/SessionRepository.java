package com.jin.chat.repository;

import com.jin.chat.common.constant.RedisKeyConst;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 在线会话与订阅关系 Redis 封装。
 * </p>
 *
 * @author jinshuai
 */
@Repository
@RequiredArgsConstructor
public class SessionRepository {

    private static final long ONLINE_TTL_MINUTES = 30;

    private final RedisTemplate<String, Object> redisTemplate;

    public void online(Long userId, String sessionId) {
        redisTemplate.opsForValue().set(RedisKeyConst.SESSION_ONLINE_PREFIX + userId,
                sessionId, ONLINE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    /** 刷新在线标记 TTL（心跳/订阅时续期） */
    public void refreshOnline(Long userId) {
        String key = RedisKeyConst.SESSION_ONLINE_PREFIX + userId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.expire(key, ONLINE_TTL_MINUTES, TimeUnit.MINUTES);
        }
    }

    public void offline(Long userId) {
        redisTemplate.delete(RedisKeyConst.SESSION_ONLINE_PREFIX + userId);
        redisTemplate.delete(RedisKeyConst.SESSION_ROOMS_PREFIX + userId);
    }

    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeyConst.SESSION_ONLINE_PREFIX + userId));
    }

    public void subscribeRoom(Long userId, Long roomId) {
        redisTemplate.opsForSet().add(RedisKeyConst.SESSION_ROOMS_PREFIX + userId, roomId);
    }

    public void unsubscribeRoom(Long userId, Long roomId) {
        redisTemplate.opsForSet().remove(RedisKeyConst.SESSION_ROOMS_PREFIX + userId, roomId);
    }

    public long onlineCount() {
        java.util.Set<String> keys = redisTemplate.keys(RedisKeyConst.SESSION_ONLINE_PREFIX + "*");
        return keys == null ? 0 : keys.size();
    }
}
