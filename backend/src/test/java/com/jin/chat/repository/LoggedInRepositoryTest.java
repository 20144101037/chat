package com.jin.chat.repository;

import com.jin.chat.common.constant.RedisKeyConst;
import com.jin.chat.common.constant.SysConfigKeys;
import com.jin.chat.service.SysConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggedInRepositoryTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SysConfigService sysConfigService;

    @Mock
    private ValueOperations<String, Object> valueOps;

    private LoggedInRepository repository;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        repository = new LoggedInRepository(redisTemplate, sysConfigService);
    }

    private void stubJwtExpireMinutes() {
        when(sysConfigService.getInt(SysConfigKeys.JWT_EXPIRE_MINUTES, SysConfigKeys.DEFAULT_JWT_EXPIRE_MINUTES))
                .thenReturn(120);
    }

    @Test
    void markLoggedIn_shouldSetKeyWithJwtTtl() {
        stubJwtExpireMinutes();
        repository.markLoggedIn(1L);
        verify(valueOps).set(eq(RedisKeyConst.LOGGED_IN_PREFIX + 1L), eq("1"), eq(120L), eq(TimeUnit.MINUTES));
    }

    @Test
    void refreshLoggedIn_shouldExpire_whenKeyExists() {
        stubJwtExpireMinutes();
        when(redisTemplate.hasKey(RedisKeyConst.LOGGED_IN_PREFIX + 2L)).thenReturn(true);
        repository.refreshLoggedIn(2L);
        verify(redisTemplate).expire(RedisKeyConst.LOGGED_IN_PREFIX + 2L, 120L, TimeUnit.MINUTES);
    }

    @Test
    void refreshLoggedIn_shouldMarkLoggedIn_whenKeyMissing() {
        stubJwtExpireMinutes();
        when(redisTemplate.hasKey(RedisKeyConst.LOGGED_IN_PREFIX + 3L)).thenReturn(false);
        repository.refreshLoggedIn(3L);
        verify(valueOps).set(eq(RedisKeyConst.LOGGED_IN_PREFIX + 3L), eq("1"), eq(120L), eq(TimeUnit.MINUTES));
    }

    @Test
    void markLoggedOut_shouldDeleteKey() {
        repository.markLoggedOut(4L);
        verify(redisTemplate).delete(RedisKeyConst.LOGGED_IN_PREFIX + 4L);
    }

    @Test
    void loggedInCount_shouldReturnKeySize() {
        when(redisTemplate.keys(RedisKeyConst.LOGGED_IN_PREFIX + "*")).thenReturn(Set.of("a", "b", "c"));
        assertEquals(3, repository.loggedInCount());
    }

    @Test
    void loggedInCount_shouldReturnZero_whenKeysNull() {
        when(redisTemplate.keys(anyString())).thenReturn(null);
        assertEquals(0, repository.loggedInCount());
    }
}
