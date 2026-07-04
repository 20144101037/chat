package com.jin.chat.controller;

import com.jin.chat.common.api.PageResult;
import com.jin.chat.common.api.ResultData;
import com.jin.chat.domain.ao.IdsAO;
import com.jin.chat.domain.ao.RoleAO;
import com.jin.chat.domain.entity.RoleDO;
import com.jin.chat.domain.query.RoleQuery;
import com.jin.chat.domain.vo.MenuVO;
import com.jin.chat.service.MenuPermissionService;
import com.jin.chat.service.MenuService;
import com.jin.chat.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 角色管理接口：分页/增删改、菜单授权（需具备角色管理菜单权限）。
 * </p>
 *
 * @author jinshuai
 */
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class SystemRoleController {

    private static final String MENU_ROLES = "/app/system/roles";
    private static final String MENU_USERS = "/app/system/users";

    private final RoleService roleService;
    private final MenuService menuService;
    private final MenuPermissionService menuPermissionService;

    @GetMapping
    public ResultData<PageResult<RoleDO>> page(RoleQuery query) {
        menuPermissionService.requireMenuPath(MENU_ROLES);
        return ResultData.success(roleService.page(query));
    }

    @GetMapping("/all")
    public ResultData<List<RoleDO>> all() {
        menuPermissionService.requireAnyMenuPath(MENU_ROLES, MENU_USERS);
        return ResultData.success(roleService.listAll());
    }

    /** 供角色分配菜单时使用（只读菜单树，不再提供菜单 CRUD 管理页） */
    @GetMapping("/menu-tree")
    public ResultData<List<MenuVO>> menuTree() {
        menuPermissionService.requireMenuPath(MENU_ROLES);
        return ResultData.success(menuService.listTree());
    }

    @PostMapping
    public ResultData<RoleDO> create(@Valid @RequestBody RoleAO ao) {
        menuPermissionService.requireMenuPath(MENU_ROLES);
        return ResultData.success(roleService.create(ao));
    }

    @PutMapping("/{id}")
    public ResultData<RoleDO> update(@PathVariable Long id, @Valid @RequestBody RoleAO ao) {
        menuPermissionService.requireMenuPath(MENU_ROLES);
        return ResultData.success(roleService.update(id, ao));
    }

    @DeleteMapping("/{id}")
    public ResultData<Void> delete(@PathVariable Long id) {
        menuPermissionService.requireMenuPath(MENU_ROLES);
        roleService.delete(id);
        return ResultData.success();
    }

    @GetMapping("/{id}/menus")
    public ResultData<List<Long>> menuIds(@PathVariable Long id) {
        menuPermissionService.requireMenuPath(MENU_ROLES);
        return ResultData.success(roleService.listMenuIds(id));
    }

    @PutMapping("/{id}/menus")
    public ResultData<Void> assignMenus(@PathVariable Long id, @RequestBody IdsAO ao) {
        menuPermissionService.requireMenuPath(MENU_ROLES);
        roleService.assignMenus(id, ao.getIds());
        return ResultData.success();
    }
}
