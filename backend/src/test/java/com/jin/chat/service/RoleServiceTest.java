package com.jin.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jin.chat.common.cache.PermissionCache;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.domain.ao.RoleAO;
import com.jin.chat.domain.entity.RoleDO;
import com.jin.chat.domain.entity.RoleMenuDO;
import com.jin.chat.mapper.RoleMapper;
import com.jin.chat.mapper.RoleMenuMapper;
import com.jin.chat.mapper.UserRoleMapper;
import com.jin.chat.service.impl.RoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleMapper roleMapper;
    @Mock
    private RoleMenuMapper roleMenuMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private PermissionCache permissionCache;

    @Spy
    @InjectMocks
    private RoleServiceImpl roleService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(roleService, "baseMapper", roleMapper);
    }

    @Test
    void create_shouldPersistCustomRole() {
        RoleAO ao = new RoleAO();
        ao.setRoleCode("BIZ_ADMIN");
        ao.setRoleName("业务管理员");
        ao.setDescription("desc");

        doReturn(0L).when(roleService).count(any(LambdaQueryWrapper.class));
        doReturn(true).when(roleService).save(any(RoleDO.class));

        RoleDO created = roleService.create(ao);
        assertEquals("BIZ_ADMIN", created.getRoleCode());
        assertFalse(created.getBuiltIn());
    }

    @Test
    void create_shouldThrow_whenCodeExists() {
        RoleAO ao = new RoleAO();
        ao.setRoleCode("USER");
        doReturn(1L).when(roleService).count(any(LambdaQueryWrapper.class));
        assertThrows(BusinessException.class, () -> roleService.create(ao));
    }

    @Test
    void update_shouldProtectBuiltInRoleCode() {
        RoleDO builtIn = new RoleDO();
        builtIn.setId(1L);
        builtIn.setRoleCode("SYS_ADMIN");
        builtIn.setBuiltIn(true);
        doReturn(builtIn).when(roleService).getById(1L);

        RoleAO ao = new RoleAO();
        ao.setRoleCode("OTHER");
        ao.setRoleName("x");
        assertThrows(BusinessException.class, () -> roleService.update(1L, ao));
    }

    @Test
    void delete_shouldRejectBuiltInRole() {
        RoleDO builtIn = new RoleDO();
        builtIn.setId(1L);
        builtIn.setBuiltIn(true);
        doReturn(builtIn).when(roleService).getById(1L);
        assertThrows(BusinessException.class, () -> roleService.delete(1L));
    }

    @Test
    void delete_shouldCascadeAndEvictCache() {
        RoleDO role = new RoleDO();
        role.setId(2L);
        role.setBuiltIn(false);
        doReturn(role).when(roleService).getById(2L);
        doReturn(true).when(roleService).removeById(2L);

        roleService.delete(2L);

        verify(roleMenuMapper).delete(any(LambdaQueryWrapper.class));
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(permissionCache).evictAll();
    }

    @Test
    void assignMenus_shouldReplaceBindings() {
        RoleDO role = new RoleDO();
        role.setId(3L);
        doReturn(role).when(roleService).getById(3L);

        roleService.assignMenus(3L, List.of(10L, 11L));

        verify(roleMenuMapper).delete(any(LambdaQueryWrapper.class));
        verify(roleMenuMapper, times(2)).insert(any(RoleMenuDO.class));
        verify(permissionCache).evictAll();
    }

    @Test
    void listMenuIds_shouldReturnIds() {
        RoleMenuDO rm = new RoleMenuDO();
        rm.setMenuId(5L);
        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rm));
        assertEquals(List.of(5L), roleService.listMenuIds(1L));
    }
}
