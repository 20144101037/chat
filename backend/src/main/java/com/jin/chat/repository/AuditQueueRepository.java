package com.jin.chat.repository;

import com.jin.chat.common.constant.RedisKeyConst;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * <p>
 * 待审核队列 Redis 封装。
 * <ul>
 *     <li>List：保证 FIFO 展示顺序（仅存消息 ID）。</li>
 *     <li>ZSet：score = 提交时间戳（毫秒），支撑超时扫描与按提交顺序处理。</li>
 * </ul>
 * </p>
 *
 * @author jinshuai
 */
@Repository
@RequiredArgsConstructor
public class AuditQueueRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 入队待审核消息。
     */
    public void enqueue(Long messageId, long submittedAtEpochMillis) {
        redisTemplate.opsForList().leftPush(RedisKeyConst.AUDIT_PENDING_QUEUE, messageId);
        redisTemplate.opsForZSet().add(RedisKeyConst.AUDIT_PENDING_ZSET, messageId, submittedAtEpochMillis);
    }

    /**
     * 出队（审核完成或超时后移除）。
     */
    public void remove(Long messageId) {
        redisTemplate.opsForList().remove(RedisKeyConst.AUDIT_PENDING_QUEUE, 0, messageId);
        redisTemplate.opsForZSet().remove(RedisKeyConst.AUDIT_PENDING_ZSET, messageId);
    }

    /**
     * 扫描提交时间早于 threshold 的超时消息 ID。
     */
    public Set<Object> scanTimeout(long thresholdEpochMillis) {
        return redisTemplate.opsForZSet()
                .rangeByScore(RedisKeyConst.AUDIT_PENDING_ZSET, 0, thresholdEpochMillis);
    }

    /**
     * 当前待审核队列长度（监控指标）。
     */
    public long pendingSize() {
        Long size = redisTemplate.opsForZSet().zCard(RedisKeyConst.AUDIT_PENDING_ZSET);
        return size == null ? 0 : size;
    }
}
