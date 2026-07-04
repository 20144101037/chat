package com.jin.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jin.chat.common.cache.PermissionCache;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.common.exception.ErrorCodeEnum;
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
import com.jin.chat.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 菜单服务实现。
 * </p>
 *
 * @author jinshuai
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<MenuMapper, MenuDO> implements MenuService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final PermissionCache permissionCache;

    @Override
    public List<MenuVO> listTree() {
        return buildTree(listAllOrdered());
    }

    @Override
    public List<MenuVO> listByUser(Long userId) {
        // 用户可见菜单读多写少，且登录高峰并发拉取，走二级缓存 + 单飞加载
        return permissionCache.getMenus(userId, () -> computeMenusForUser(userId));
    }

    private List<MenuVO> computeMenusForUser(Long userId) {
        List<Long> roleIds = cachedRoleIds(userId);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<RoleDO> roles = roleMapper.selectBatchIds(roleIds);
        boolean sysAdmin = roles.stream()
                .anyMatch(r -> UserRoleEnum.SYS_ADMIN.name().equals(r.getRoleCode()));

        List<MenuDO> menus;
        if (sysAdmin) {
            // 系统管理员可见全部菜单
            menus = listAllOrdered();
        } else {
            List<Long> menuIds = roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenuDO>()
                            .in(RoleMenuDO::getRoleId, roleIds))
                    .stream().map(RoleMenuDO::getMenuId).distinct().collect(Collectors.toList());
            if (menuIds.isEmpty()) {
                return new ArrayList<>();
            }
            menus = includeAncestors(menuIds);
        }
        return buildTree(menus);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public MenuDO create(MenuAO ao) {
        if (existsKey(ao.getMenuKey(), null)) {
            throw new BusinessException(ErrorCodeEnum.MENU_KEY_EXIST);
        }
        MenuDO menu = new MenuDO();
        applyAO(menu, ao);
        save(menu);
        // 菜单结构变更影响所有用户的可见菜单
        permissionCache.evictAll();
        return menu;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public MenuDO update(Long id, MenuAO ao) {
        MenuDO menu = getById(id);
        if (menu == null) {
            throw new BusinessException(ErrorCodeEnum.MENU_NOT_EXIST);
        }
        if (existsKey(ao.getMenuKey(), id)) {
            throw new BusinessException(ErrorCodeEnum.MENU_KEY_EXIST);
        }
        applyAO(menu, ao);
        updateById(menu);
        permissionCache.evictAll();
        return menu;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long id) {
        MenuDO menu = getById(id);
        if (menu == null) {
            throw new BusinessException(ErrorCodeEnum.MENU_NOT_EXIST);
        }
        long children = count(new LambdaQueryWrapper<MenuDO>().eq(MenuDO::getParentId, id));
        if (children > 0) {
            throw new BusinessException(ErrorCodeEnum.MENU_HAS_CHILDREN);
        }
        removeById(id);
        // 同步清理角色-菜单授权
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenuDO>().eq(RoleMenuDO::getMenuId, id));
        permissionCache.evictAll();
    }

    private void applyAO(MenuDO menu, MenuAO ao) {
        menu.setParentId(ao.getParentId() == null ? 0L : ao.getParentId());
        menu.setMenuKey(ao.getMenuKey());
        menu.setName(ao.getName());
        menu.setPath(ao.getPath());
        menu.setSort(ao.getSort() == null ? 0 : ao.getSort());
        menu.setMenuType(ao.getMenuType() == null ? "MENU" : ao.getMenuType());
    }

    private boolean existsKey(String key, Long excludeId) {
        return count(new LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getMenuKey, key)
                .ne(excludeId != null, MenuDO::getId, excludeId)) > 0;
    }

    private List<Long> cachedRoleIds(Long userId) {
        // 包一层 ArrayList，保证序列化到 Redis 后可反序列化为具体集合类型
        return permissionCache.getRoleIds(userId, () -> new ArrayList<>(resolveRoleIds(userId)));
    }

    private List<Long> resolveRoleIds(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleDO>()
                        .eq(UserRoleDO::getUserId, userId))
                .stream().map(UserRoleDO::getRoleId).distinct().collect(Collectors.toList());
        if (!roleIds.isEmpty()) {
            return roleIds;
        }
        // 兼容尚未建立 user_roles 关联的历史用户：回退到 users.role
        UserDO user = userMapper.selectById(userId);
        if (user == null || user.getRole() == null) {
            return Collections.emptyList();
        }
        RoleDO role = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getRoleCode, user.getRole()).last("limit 1"));
        return role == null ? Collections.emptyList() : List.of(role.getId());
    }

    private List<MenuDO> listAllOrdered() {
        return list(new LambdaQueryWrapper<MenuDO>()
                .orderByAsc(MenuDO::getParentId)
                .orderByAsc(MenuDO::getSort));
    }

    /**
     * 将菜单及其所有祖先菜单纳入，保证子菜单能挂在其目录下展示。
     */
    private List<MenuDO> includeAncestors(List<Long> menuIds) {
        Map<Long, MenuDO> all = listAllOrdered().stream()
                .collect(Collectors.toMap(MenuDO::getId, m -> m, (a, b) -> a, LinkedHashMap::new));
        Map<Long, MenuDO> result = new LinkedHashMap<>();
        for (Long id : menuIds) {
            MenuDO cur = all.get(id);
            while (cur != null && !result.containsKey(cur.getId())) {
                result.put(cur.getId(), cur);
                Long parentId = cur.getParentId();
                cur = (parentId == null || parentId == 0L) ? null : all.get(parentId);
            }
        }
        return result.values().stream()
                .sorted(Comparator.comparing((MenuDO m) -> m.getParentId() == null ? 0L : m.getParentId())
                        .thenComparing(m -> m.getSort() == null ? 0 : m.getSort()))
                .collect(Collectors.toList());
    }

    private List<MenuVO> buildTree(List<MenuDO> menus) {
        Map<Long, MenuVO> voMap = new LinkedHashMap<>();
        for (MenuDO m : menus) {
            voMap.put(m.getId(), toVO(m));
        }
        Set<Long> ids = voMap.keySet();
        List<MenuVO> roots = new ArrayList<>();
        for (MenuDO m : menus) {
            MenuVO vo = voMap.get(m.getId());
            Long parentId = m.getParentId();
            if (parentId == null || parentId == 0L || !ids.contains(parentId)) {
                roots.add(vo);
            } else {
                voMap.get(parentId).getChildren().add(vo);
            }
        }
        sortTree(roots);
        return roots;
    }

    private void sortTree(List<MenuVO> nodes) {
        nodes.sort(Comparator.comparing(v -> v.getSort() == null ? 0 : v.getSort()));
        for (MenuVO node : nodes) {
            if (!node.getChildren().isEmpty()) {
                sortTree(node.getChildren());
            }
        }
    }

    private MenuVO toVO(MenuDO m) {
        MenuVO vo = new MenuVO();
        vo.setId(m.getId());
        vo.setParentId(m.getParentId());
        vo.setMenuKey(m.getMenuKey());
        vo.setName(m.getName());
        vo.setPath(m.getPath());
        vo.setSort(m.getSort());
        vo.setMenuType(m.getMenuType());
        return vo;
    }
}
