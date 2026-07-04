package com.jin.chat.repository;

import com.jin.chat.common.constant.RedisKeyConst;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 令牌有效性仓储：基于「用户最小有效签发时间」实现无状态 JWT 的强制失效（重置密码/强制下线）。
 * 早于该时间签发的令牌一律视为失效。
 * </p>
 *
 * @author jinshuai
 */
@Repository
@RequiredArgsConstructor
public class TokenValidityRepository {

    /** 失效标记保留时长：需覆盖最长令牌有效期，取较大冗余值即可。 */
    private static final long TTL_DAYS = 30;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 使该用户在此刻之前签发的所有令牌失效。
     */
    public void invalidateBefore(Long userId, long epochMillis) {
        redisTemplate.opsForValue().set(RedisKeyConst.TOKEN_MIN_IAT_PREFIX + userId,
                String.valueOf(epochMillis), TTL_DAYS, TimeUnit.DAYS);
    }

    /**
     * 校验令牌是否仍然有效：签发时间不早于用户的最小有效签发时间。
     *
     * @param issuedAtMillis 令牌签发时间（epoch millis），为 null 时视为无效
     */
    public boolean isValid(Long userId, Long issuedAtMillis) {
        if (issuedAtMillis == null) {
            return false;
        }
        Object min = redisTemplate.opsForValue().get(RedisKeyConst.TOKEN_MIN_IAT_PREFIX + userId);
        if (min == null) {
            return true;
        }
        long minIat = Long.parseLong(String.valueOf(min));
        // JWT 的 iat 精确到秒，允许同秒签发的令牌通过，避免误伤刚签发的令牌
        return issuedAtMillis + 999 >= minIat;
    }
}
