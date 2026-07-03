package com.jin.chat.common.util;

import com.jin.chat.common.context.LoginUser;
import com.jin.chat.service.SysConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private static final String SECRET = "test-jwt-secret-key-at-least-32-bytes-long!!";

    @Mock
    private SysConfigService sysConfigService;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(sysConfigService);
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        jwtUtil.init();
        when(sysConfigService.getInt(eq(JwtUtil.CONFIG_KEY_EXPIRE_MINUTES), anyInt())).thenReturn(120);
    }

    @Test
    void generateAndParse_shouldRoundTrip() {
        LoginUser user = new LoginUser();
        user.setUserId(42L);
        user.setUsername("lisi");
        user.setRole("USER");

        String token = jwtUtil.generateToken(user);
        LoginUser parsed = jwtUtil.parseToken(token);

        assertNotNull(parsed);
        assertEquals(42L, parsed.getUserId());
        assertEquals("lisi", parsed.getUsername());
        assertEquals("USER", parsed.getRole());
        assertNotNull(parsed.getIssuedAt());
    }

    @Test
    void parseToken_shouldReturnNull_whenTampered() {
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setUsername("a");
        user.setRole("USER");
        String token = jwtUtil.generateToken(user);
        assertNull(jwtUtil.parseToken(token + "x"));
    }

    @Test
    void parseToken_shouldReturnNull_whenInvalid() {
        assertNull(jwtUtil.parseToken("not-a-jwt"));
    }
}
