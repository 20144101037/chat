package com.jin.chat.repository;

import com.jin.chat.common.constant.RedisKeyConst;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenValidityRepositoryTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    private TokenValidityRepository repository;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        repository = new TokenValidityRepository(redisTemplate);
    }

    @Test
    void isValid_shouldReturnTrue_whenNoInvalidateRecord() {
        when(valueOps.get(RedisKeyConst.TOKEN_MIN_IAT_PREFIX + 1L)).thenReturn(null);
        assertTrue(repository.isValid(1L, 1_700_000_000_000L));
    }

    @Test
    void isValid_shouldReturnFalse_whenIssuedAtNull() {
        assertFalse(repository.isValid(1L, null));
    }

    @Test
    void isValid_shouldReturnFalse_whenTokenIssuedBeforeMinIat() {
        when(valueOps.get(RedisKeyConst.TOKEN_MIN_IAT_PREFIX + 1L)).thenReturn("2000");
        assertFalse(repository.isValid(1L, 1000L));
    }

    @Test
    void isValid_shouldAllowSameSecondIssuedToken() {
        when(valueOps.get(RedisKeyConst.TOKEN_MIN_IAT_PREFIX + 1L)).thenReturn("1000");
        assertTrue(repository.isValid(1L, 1000L));
    }

    @Test
    void invalidateBefore_shouldStoreEpochMillis() {
        repository.invalidateBefore(9L, 12345L);
        verify(valueOps).set(eq(RedisKeyConst.TOKEN_MIN_IAT_PREFIX + 9L), eq("12345"), eq(30L), eq(TimeUnit.DAYS));
    }
}
