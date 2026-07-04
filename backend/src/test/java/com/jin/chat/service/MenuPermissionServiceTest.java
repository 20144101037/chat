package com.jin.chat.service;

import com.jin.chat.common.context.LoginUser;
import com.jin.chat.common.context.UserContextHolder;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.domain.vo.MenuVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuPermissionServiceTest {

    @Mock
    private MenuService menuService;

    @InjectMocks
    private MenuPermissionService menuPermissionService;

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void hasMenuPath_shouldCollectNestedPaths() {
        MenuVO child = menu("/app/system/users", "system:users");
        MenuVO parent = dir("system");
        parent.setChildren(List.of(child));
        when(menuService.listByUser(1L)).thenReturn(List.of(parent));

        assertTrue(menuPermissionService.hasMenuPath(1L, "/app/system/users"));
        assertFalse(menuPermissionService.hasMenuPath(1L, "/app/audit"));
        assertFalse(menuPermissionService.hasMenuPath(1L, ""));
        assertFalse(menuPermissionService.hasMenuPath(1L, null));
    }

    @Test
    void requireMenuPath_shouldThrow_whenDenied() {
        LoginUser user = new LoginUser();
        user.setUserId(2L);
        UserContextHolder.set(user);
        when(menuService.listByUser(2L)).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> menuPermissionService.requireMenuPath("/app/system/users"));
    }

    @Test
    void requireAnyMenuPath_shouldPass_whenOneMatches() {
        LoginUser user = new LoginUser();
        user.setUserId(3L);
        UserContextHolder.set(user);
        when(menuService.listByUser(3L)).thenReturn(List.of(menu("/app/system/users", "system:users")));

        assertDoesNotThrow(() -> menuPermissionService.requireAnyMenuPath("/app/system/roles", "/app/system/users"));
    }

    @Test
    void requireAnyMenuPath_shouldThrow_whenNoneMatch() {
        LoginUser user = new LoginUser();
        user.setUserId(4L);
        UserContextHolder.set(user);
        when(menuService.listByUser(4L)).thenReturn(List.of());

        assertThrows(BusinessException.class,
                () -> menuPermissionService.requireAnyMenuPath("/app/system/roles", "/app/system/users"));
    }

    private static MenuVO menu(String path, String key) {
        MenuVO vo = new MenuVO();
        vo.setPath(path);
        vo.setMenuKey(key);
        return vo;
    }

    private static MenuVO dir(String key) {
        MenuVO vo = new MenuVO();
        vo.setMenuKey(key);
        vo.setMenuType("DIR");
        return vo;
    }
}
