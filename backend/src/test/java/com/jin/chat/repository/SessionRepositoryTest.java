package com.jin.chat.repository;

import com.jin.chat.common.constant.RedisKeyConst;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionRepositoryTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @Mock
    private SetOperations<String, Object> setOps;

    private SessionRepository repository;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        repository = new SessionRepository(redisTemplate);
    }

    @Test
    void online_shouldSetKeyWithTtl() {
        repository.online(1L, "sess-1");
        verify(valueOps).set(eq(RedisKeyConst.SESSION_ONLINE_PREFIX + 1L), eq("sess-1"), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    void offline_shouldDeleteOnlineAndRooms() {
        repository.offline(2L);
        verify(redisTemplate).delete(RedisKeyConst.SESSION_ONLINE_PREFIX + 2L);
        verify(redisTemplate).delete(RedisKeyConst.SESSION_ROOMS_PREFIX + 2L);
    }

    @Test
    void isOnline_shouldReflectRedis() {
        when(redisTemplate.hasKey(RedisKeyConst.SESSION_ONLINE_PREFIX + 3L)).thenReturn(true);
        assertTrue(repository.isOnline(3L));
    }

    @Test
    void subscribeAndUnsubscribeRoom() {
        repository.subscribeRoom(1L, 10L);
        verify(setOps).add(RedisKeyConst.SESSION_ROOMS_PREFIX + 1L, 10L);
        repository.unsubscribeRoom(1L, 10L);
        verify(setOps).remove(RedisKeyConst.SESSION_ROOMS_PREFIX + 1L, 10L);
    }

    @Test
    void onlineCount_shouldReturnKeySize() {
        when(redisTemplate.keys(RedisKeyConst.SESSION_ONLINE_PREFIX + "*")).thenReturn(Set.of("a", "b"));
        assertEquals(2, repository.onlineCount());
    }

    @Test
    void onlineCount_shouldReturnZero_whenKeysNull() {
        when(redisTemplate.keys(anyString())).thenReturn(null);
        assertEquals(0, repository.onlineCount());
    }
}
