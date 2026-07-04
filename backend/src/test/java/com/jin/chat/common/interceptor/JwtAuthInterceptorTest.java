package com.jin.chat.common.interceptor;

import com.jin.chat.common.context.LoginUser;
import com.jin.chat.common.context.UserContextHolder;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.common.util.JwtUtil;
import com.jin.chat.repository.LoggedInRepository;
import com.jin.chat.repository.TokenValidityRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private TokenValidityRepository tokenValidityRepository;
    @Mock
    private LoggedInRepository loggedInRepository;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private JwtAuthInterceptor interceptor;

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void preHandle_shouldRejectMissingHeader() {
        when(request.getHeader("Authorization")).thenReturn(null);
        assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void preHandle_shouldSetContext_whenTokenValid() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer good-token");
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setIssuedAt(1000L);
        when(jwtUtil.parseToken("good-token")).thenReturn(user);
        when(tokenValidityRepository.isValid(1L, 1000L)).thenReturn(true);

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals(1L, UserContextHolder.currentUserId());
        verify(loggedInRepository).refreshLoggedIn(1L);

        interceptor.afterCompletion(request, response, new Object(), null);
        assertNull(UserContextHolder.get());
    }

    @Test
    void preHandle_shouldRejectInvalidatedToken() {
        when(request.getHeader("Authorization")).thenReturn("Bearer old-token");
        LoginUser user = new LoginUser();
        user.setUserId(2L);
        user.setIssuedAt(100L);
        when(jwtUtil.parseToken("old-token")).thenReturn(user);
        when(tokenValidityRepository.isValid(2L, 100L)).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, response, new Object()));
    }
}
