package com.jin.chat.common.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistributedLockTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    private DistributedLock distributedLock;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        distributedLock = new DistributedLock(redisTemplate);
    }

    @Test
    void tryLock_shouldReturnTrue_whenSetIfAbsentSucceeds() {
        when(valueOps.setIfAbsent(eq("lock:1"), eq("token-a"), any(Duration.class))).thenReturn(true);
        assertTrue(distributedLock.tryLock("lock:1", "token-a", Duration.ofSeconds(10)));
    }

    @Test
    void tryLock_shouldReturnFalse_whenAlreadyLocked() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        assertFalse(distributedLock.tryLock("lock:1", "token-a", Duration.ofSeconds(10)));
    }

    @Test
    void unlock_shouldExecuteLuaScript() {
        distributedLock.unlock("lock:1", "token-a");
        verify(redisTemplate).execute(any(), eq(java.util.Collections.singletonList("lock:1")), eq("token-a"));
    }

    @Test
    void unlock_shouldNotThrow_whenRedisFails() {
        doThrow(new RuntimeException("redis down")).when(redisTemplate).execute(any(), anyList(), any());
        assertDoesNotThrow(() -> distributedLock.unlock("lock:1", "token-a"));
    }
}
