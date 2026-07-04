package com.jin.chat.controller;

import com.jin.chat.common.api.ResultData;
import com.jin.chat.domain.ao.MenuAO;
import com.jin.chat.domain.entity.MenuDO;
import com.jin.chat.domain.vo.MenuVO;
import com.jin.chat.service.MenuPermissionService;
import com.jin.chat.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 菜单权限管理接口：菜单树、增删改（需具备菜单权限管理菜单）。
 * </p>
 *
 * @author jinshuai
 */
@RestController
@RequestMapping("/api/admin/menus")
@RequiredArgsConstructor
public class SystemMenuController {

    private static final String MENU_MENUS = "/app/system/menus";
    private static final String MENU_ROLES = "/app/system/roles";

    private final MenuService menuService;
    private final MenuPermissionService menuPermissionService;

    @GetMapping("/tree")
    public ResultData<List<MenuVO>> tree() {
        menuPermissionService.requireAnyMenuPath(MENU_MENUS, MENU_ROLES);
        return ResultData.success(menuService.listTree());
    }

    @PostMapping
    public ResultData<MenuDO> create(@Valid @RequestBody MenuAO ao) {
        menuPermissionService.requireMenuPath(MENU_MENUS);
        return ResultData.success(menuService.create(ao));
    }

    @PutMapping("/{id}")
    public ResultData<MenuDO> update(@PathVariable Long id, @Valid @RequestBody MenuAO ao) {
        menuPermissionService.requireMenuPath(MENU_MENUS);
        return ResultData.success(menuService.update(id, ao));
    }

    @DeleteMapping("/{id}")
    public ResultData<Void> delete(@PathVariable Long id) {
        menuPermissionService.requireMenuPath(MENU_MENUS);
        menuService.delete(id);
        return ResultData.success();
    }
}
