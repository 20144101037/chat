package com.jin.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jin.chat.common.cache.PermissionCache;
import com.jin.chat.common.context.LoginUser;
import com.jin.chat.common.context.UserContextHolder;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.domain.ao.RoleAO;
import com.jin.chat.domain.entity.RoleDO;
import com.jin.chat.domain.entity.UserDO;
import com.jin.chat.domain.entity.UserRoleDO;
import com.jin.chat.domain.enums.UserRoleEnum;
import com.jin.chat.domain.query.UserQuery;
import com.jin.chat.mapper.RoleMapper;
import com.jin.chat.mapper.UserMapper;
import com.jin.chat.mapper.UserRoleMapper;
import com.jin.chat.repository.SessionRepository;
import com.jin.chat.repository.TokenValidityRepository;
import com.jin.chat.service.impl.UserAdminServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private PermissionCache permissionCache;
    @Mock
    private TokenValidityRepository tokenValidityRepository;
    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private UserAdminServiceImpl userAdminService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userAdminService, "passwordEncoder", passwordEncoder);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void assignRoles_shouldSyncPrimaryRoleAndEvictCache() {
        UserDO user = new UserDO();
        user.setId(10L);
        user.setRole("USER");
        when(userMapper.selectById(10L)).thenReturn(user);

        RoleDO userRole = role("USER", 1L);
        RoleDO adminRole = role("ROOM_ADMIN", 2L);
        when(roleMapper.selectList(null)).thenReturn(List.of(userRole, adminRole));

        userAdminService.assignRoles(10L, List.of(1L, 2L));

        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(userRoleMapper, times(2)).insert(any(UserRoleDO.class));
        verify(userMapper).updateById(argThat(u -> UserRoleEnum.ROOM_ADMIN.name().equals(u.getRole())));
        verify(permissionCache).evictUser(10L);
    }

    @Test
    void assignRoles_shouldFallbackUser_whenEmptyRoles() {
        UserDO user = new UserDO();
        user.setId(11L);
        when(userMapper.selectById(11L)).thenReturn(user);
        when(roleMapper.selectList(null)).thenReturn(List.of());

        userAdminService.assignRoles(11L, List.of());

        verify(userMapper).updateById(argThat(u -> UserRoleEnum.USER.name().equals(u.getRole())));
    }

    @Test
    void assignRoles_shouldThrow_whenUserMissing() {
        when(userMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> userAdminService.assignRoles(99L, List.of(1L)));
    }

    @Test
    void updateStatus_shouldPersist() {
        UserDO user = new UserDO();
        user.setId(5L);
        when(userMapper.selectById(5L)).thenReturn(user);

        userAdminService.updateStatus(5L, "BANNED");

        assertEquals("BANNED", user.getStatus());
        verify(userMapper).updateById(user);
    }

    @Test
    void resetPassword_shouldEncodeAndInvalidateTokenAfterCommit() {
        UserDO user = new UserDO();
        user.setId(7L);
        when(userMapper.selectById(7L)).thenReturn(user);

        userAdminService.resetPassword(7L, "newpass123");

        assertTrue(passwordEncoder.matches("newpass123", user.getPasswordHash()));
        verify(userMapper).updateById(user);

        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(tokenValidityRepository).invalidateBefore(userIdCaptor.capture(), anyLong());
        assertEquals(7L, userIdCaptor.getValue());
        verify(sessionRepository).offline(7L);
    }

    @Test
    void listRoleIds_shouldReturnIds() {
        UserRoleDO ur = new UserRoleDO();
        ur.setRoleId(3L);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(ur));
        assertEquals(List.of(3L), userAdminService.listRoleIds(1L));
    }

    @Test
    void page_shouldMapRoleNames() {
        UserDO user = new UserDO();
        user.setId(1L);
        user.setUsername("alice");
        user.setNickname("Alice");
        user.setRole("USER");
        user.setStatus("ACTIVE");

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserDO> mpPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10, 1);
        mpPage.setRecords(List.of(user));
        when(userMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(mpPage);

        UserRoleDO ur = new UserRoleDO();
        ur.setUserId(1L);
        ur.setRoleId(1L);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(ur));

        RoleDO role = role("USER", 1L);
        role.setRoleName("普通用户");
        when(roleMapper.selectList(null)).thenReturn(List.of(role));

        UserQuery query = new UserQuery();
        query.setPageNo(1);
        query.setPageSize(10);
        assertEquals(1, userAdminService.page(query).getTotal());
    }

    private static RoleDO role(String code, Long id) {
        RoleDO role = new RoleDO();
        role.setId(id);
        role.setRoleCode(code);
        role.setRoleName(code);
        return role;
    }
}
