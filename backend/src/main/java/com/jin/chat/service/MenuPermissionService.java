package com.jin.chat.service;

import com.jin.chat.common.context.UserContextHolder;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.common.exception.ErrorCodeEnum;
import com.jin.chat.domain.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 菜单路径权限校验：按用户 RBAC 可见菜单判断是否可访问接口/功能，替代硬编码角色判断。
 * </p>
 *
 * @author jinshuai
 */
@Service
@RequiredArgsConstructor
public class MenuPermissionService {

    private final MenuService menuService;

    public boolean hasMenuPath(Long userId, String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        return collectPaths(menuService.listByUser(userId)).contains(path);
    }

    public void requireMenuPath(String path) {
        if (!hasMenuPath(UserContextHolder.currentUserId(), path)) {
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN);
        }
    }

    /**
     * 满足任一菜单路径即可（如用户管理页需拉取角色列表）。
     */
    public void requireAnyMenuPath(String... paths) {
        Long userId = UserContextHolder.currentUserId();
        Set<String> allowed = collectPaths(menuService.listByUser(userId));
        for (String path : paths) {
            if (allowed.contains(path)) {
                return;
            }
        }
        throw new BusinessException(ErrorCodeEnum.FORBIDDEN);
    }

    private Set<String> collectPaths(List<MenuVO> menus) {
        Set<String> paths = new HashSet<>();
        collectPaths(menus, paths);
        return paths;
    }

    private void collectPaths(List<MenuVO> menus, Set<String> paths) {
        if (menus == null) {
            return;
        }
        for (MenuVO menu : menus) {
            if (StringUtils.hasText(menu.getPath())) {
                paths.add(menu.getPath());
            }
            collectPaths(menu.getChildren(), paths);
        }
    }
}
