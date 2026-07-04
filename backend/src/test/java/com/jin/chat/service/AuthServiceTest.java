package com.jin.chat.service;

import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.common.util.JwtUtil;
import com.jin.chat.domain.ao.LoginAO;
import com.jin.chat.domain.ao.RegisterAO;
import com.jin.chat.domain.entity.RoleDO;
import com.jin.chat.domain.entity.UserDO;
import com.jin.chat.domain.vo.LoginVO;
import com.jin.chat.domain.vo.UserVO;
import com.jin.chat.mapper.RoleMapper;
import com.jin.chat.mapper.UserMapper;
import com.jin.chat.mapper.UserRoleMapper;
import com.jin.chat.repository.LoggedInRepository;
import com.jin.chat.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>
 * AuthService 单元测试（示例，演示测试结构）。
 * </p>
 *
 * @author jinshuai
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private LoggedInRepository loggedInRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        // 用真实 BCrypt，替换 @InjectMocks 注入的 mock
        ReflectionTestUtils.setField(authService, "passwordEncoder", passwordEncoder);
    }

    @Test
    void register_shouldSucceed_whenUsernameNotExist() {
        when(userMapper.selectByUsername("alice")).thenReturn(null);
        when(userMapper.insert(any(UserDO.class))).thenAnswer(inv -> {
            UserDO u = inv.getArgument(0);
            u.setId(1L);
            return 1;
        });
        RoleDO role = new RoleDO();
        role.setId(1L);
        role.setRoleCode("USER");
        when(roleMapper.selectOne(any())).thenReturn(role);
        when(userRoleMapper.selectCount(any())).thenReturn(0L);

        RegisterAO ao = new RegisterAO();
        ao.setUsername("alice");
        ao.setPassword("secret123");

        UserVO vo = authService.register(ao);
        assertEquals("alice", vo.getUsername());
        assertEquals("USER", vo.getRole());
    }

    @Test
    void register_shouldFail_whenUsernameExist() {
        when(userMapper.selectByUsername("bob")).thenReturn(new UserDO());
        RegisterAO ao = new RegisterAO();
        ao.setUsername("bob");
        ao.setPassword("secret123");

        assertThrows(BusinessException.class, () -> authService.register(ao));
    }

    @Test
    void login_shouldReturnToken_whenPasswordMatches() {
        UserDO user = new UserDO();
        user.setId(1L);
        user.setUsername("carol");
        user.setRole("USER");
        user.setPasswordHash(passwordEncoder.encode("mypassword"));
        when(userMapper.selectByUsername("carol")).thenReturn(user);
        when(jwtUtil.generateToken(any())).thenReturn("mock-token");

        LoginAO ao = new LoginAO();
        ao.setUsername("carol");
        ao.setPassword("mypassword");

        LoginVO vo = authService.login(ao);
        assertEquals("mock-token", vo.getToken());
        assertEquals("carol", vo.getUser().getUsername());
        verify(loggedInRepository).markLoggedIn(1L);
    }

    @Test
    void logout_shouldMarkLoggedOut() {
        authService.logout(5L);
        verify(loggedInRepository).markLoggedOut(5L);
    }

    @Test
    void login_shouldFail_whenPasswordWrong() {
        UserDO user = new UserDO();
        user.setUsername("dave");
        user.setPasswordHash(passwordEncoder.encode("correct"));
        when(userMapper.selectByUsername("dave")).thenReturn(user);

        LoginAO ao = new LoginAO();
        ao.setUsername("dave");
        ao.setPassword("wrong");

        assertThrows(BusinessException.class, () -> authService.login(ao));
    }

    @Test
    void login_shouldFail_whenUserNotExist() {
        when(userMapper.selectByUsername("ghost")).thenReturn(null);
        LoginAO ao = new LoginAO();
        ao.setUsername("ghost");
        ao.setPassword("any");
        assertThrows(BusinessException.class, () -> authService.login(ao));
    }
}
