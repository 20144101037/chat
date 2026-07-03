package com.jin.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jin.chat.common.cache.PermissionCache;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.domain.ao.MenuAO;
import com.jin.chat.domain.entity.MenuDO;
import com.jin.chat.domain.entity.RoleDO;
import com.jin.chat.domain.entity.RoleMenuDO;
import com.jin.chat.domain.entity.UserDO;
import com.jin.chat.domain.entity.UserRoleDO;
import com.jin.chat.domain.enums.UserRoleEnum;
import com.jin.chat.domain.vo.MenuVO;
import com.jin.chat.mapper.MenuMapper;
import com.jin.chat.mapper.RoleMapper;
import com.jin.chat.mapper.RoleMenuMapper;
import com.jin.chat.mapper.UserMapper;
import com.jin.chat.mapper.UserRoleMapper;
import com.jin.chat.service.impl.MenuServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuMapper menuMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private UserRoleMapper userRoleMapper;
    @Mock
    private RoleMenuMapper roleMenuMapper;
    @Mock
    private PermissionCache permissionCache;

    @Spy
    @InjectMocks
    private MenuServiceImpl menuService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(menuService, "baseMapper", menuMapper);
    }

    @Test
    void listByUser_shouldDelegateToCache() {
        List<MenuVO> expected = List.of(new MenuVO());
        when(permissionCache.getMenus(eq(1L), any())).thenReturn(expected);
        assertSame(expected, menuService.listByUser(1L));
    }

    @Test
    void listByUser_sysAdmin_shouldReturnAllMenus() {
        when(permissionCache.getMenus(eq(2L), any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Supplier<List<MenuVO>> loader = inv.getArgument(1);
            return loader.get();
        });
        UserRoleDO ur = new UserRoleDO();
        ur.setRoleId(10L);
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(ur));
        RoleDO sysAdmin = new RoleDO();
        sysAdmin.setId(10L);
        sysAdmin.setRoleCode(UserRoleEnum.SYS_ADMIN.name());
        when(roleMapper.selectBatchIds(anyCollection())).thenReturn(List.of(sysAdmin));

        MenuDO menu = new MenuDO();
        menu.setId(1L);
        menu.setParentId(0L);
        menu.setMenuKey("rooms");
        menu.setName("聊天室");
        menu.setPath("/app/rooms");
        menu.setSort(1);
        doReturn(List.of(menu)).when(menuService).list(any(LambdaQueryWrapper.class));

        List<MenuVO> menus = menuService.listByUser(2L);
        assertEquals(1, menus.size());
        assertEquals("/app/rooms", menus.get(0).getPath());
    }

    @Test
    void create_shouldRejectDuplicateKey() {
        MenuAO ao = new MenuAO();
        ao.setMenuKey("rooms");
        ao.setName("聊天室");
        doReturn(1L).when(menuService).count(any(LambdaQueryWrapper.class));
        assertThrows(BusinessException.class, () -> menuService.create(ao));
    }

    @Test
    void delete_shouldRejectWhenHasChildren() {
        MenuDO menu = new MenuDO();
        menu.setId(5L);
        doReturn(menu).when(menuService).getById(5L);
        doReturn(1L).when(menuService).count(any(LambdaQueryWrapper.class));
        assertThrows(BusinessException.class, () -> menuService.delete(5L));
    }

    @Test
    void delete_shouldRemoveRoleMenusAndEvictCache() {
        MenuDO menu = new MenuDO();
        menu.setId(6L);
        doReturn(menu).when(menuService).getById(6L);
        doReturn(0L).when(menuService).count(any(LambdaQueryWrapper.class));
        doReturn(true).when(menuService).removeById(6L);

        menuService.delete(6L);

        verify(roleMenuMapper).delete(any(LambdaQueryWrapper.class));
        verify(permissionCache).evictAll();
    }

    @Test
    void listByUser_shouldFallbackLegacyUserRole() {
        when(permissionCache.getMenus(eq(3L), any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Supplier<List<MenuVO>> loader = inv.getArgument(1);
            return loader.get();
        });
        when(userRoleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        UserDO user = new UserDO();
        user.setId(3L);
        user.setRole(UserRoleEnum.USER.name());
        when(userMapper.selectById(3L)).thenReturn(user);
        RoleDO role = new RoleDO();
        role.setId(20L);
        role.setRoleCode(UserRoleEnum.USER.name());
        when(roleMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(role);
        when(roleMapper.selectBatchIds(anyCollection())).thenReturn(List.of(role));

        RoleMenuDO rm = new RoleMenuDO();
        rm.setMenuId(1L);
        when(roleMenuMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rm));

        MenuDO m = new MenuDO();
        m.setId(1L);
        m.setParentId(0L);
        m.setMenuKey("rooms");
        m.setName("聊天室");
        m.setPath("/app/rooms");
        m.setSort(1);
        doReturn(List.of(m)).when(menuService).list(any(LambdaQueryWrapper.class));

        assertFalse(menuService.listByUser(3L).isEmpty());
    }
}
