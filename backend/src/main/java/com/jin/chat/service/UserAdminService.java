package com.jin.chat.service;

import com.jin.chat.common.api.PageResult;
import com.jin.chat.domain.query.UserQuery;
import com.jin.chat.domain.vo.AdminUserVO;

import java.util.List;

/**
 * <p>
 * 用户管理服务：分页查询、为用户分配角色、启用/封禁。
 * </p>
 *
 * @author jinshuai
 */
public interface UserAdminService {

    PageResult<AdminUserVO> page(UserQuery query);

    List<Long> listRoleIds(Long userId);

    /**
     * 覆盖设置用户的角色，并同步 users.role 为最高权限角色（用于粗粒度鉴权与 JWT）。
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 更新用户状态（ACTIVE / BANNED）。
     */
    void updateStatus(Long userId, String status);

    /**
     * 管理员重置用户密码（明文将被加密存储）。
     */
    void resetPassword(Long userId, String rawPassword);
}
