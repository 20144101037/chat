package com.jin.chat.controller;

import com.jin.chat.common.api.PageResult;
import com.jin.chat.common.api.ResultData;
import com.jin.chat.domain.ao.IdsAO;
import com.jin.chat.domain.ao.ResetPasswordAO;
import com.jin.chat.domain.query.UserQuery;
import com.jin.chat.domain.vo.AdminUserVO;
import com.jin.chat.service.MenuPermissionService;
import com.jin.chat.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户管理接口（仅系统管理员）：分页、分配角色、启用/封禁。
 * </p>
 *
 * @author jinshuai
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class SystemUserController {

    private static final String MENU_USERS = "/app/system/users";

    private final UserAdminService userAdminService;
    private final MenuPermissionService menuPermissionService;

    @GetMapping
    public ResultData<PageResult<AdminUserVO>> page(UserQuery query) {
        assertUserManage();
        return ResultData.success(userAdminService.page(query));
    }

    @GetMapping("/{id}/roles")
    public ResultData<List<Long>> roles(@PathVariable Long id) {
        assertUserManage();
        return ResultData.success(userAdminService.listRoleIds(id));
    }

    @PutMapping("/{id}/roles")
    public ResultData<Void> assignRoles(@PathVariable Long id, @RequestBody IdsAO ao) {
        assertUserManage();
        userAdminService.assignRoles(id, ao.getIds());
        return ResultData.success();
    }

    @PatchMapping("/{id}/status")
    public ResultData<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        assertUserManage();
        userAdminService.updateStatus(id, body.get("status"));
        return ResultData.success();
    }

    @PutMapping("/{id}/password")
    public ResultData<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordAO ao) {
        assertUserManage();
        userAdminService.resetPassword(id, ao.getPassword());
        return ResultData.success();
    }

    private void assertUserManage() {
        menuPermissionService.requireMenuPath(MENU_USERS);
    }
}
