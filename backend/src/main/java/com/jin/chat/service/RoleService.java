package com.jin.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jin.chat.common.api.PageResult;
import com.jin.chat.domain.ao.RoleAO;
import com.jin.chat.domain.entity.RoleDO;
import com.jin.chat.domain.query.RoleQuery;

import java.util.List;

/**
 * <p>
 * 角色服务：分页/增删改，以及角色-菜单授权维护。
 * </p>
 *
 * @author jinshuai
 */
public interface RoleService extends IService<RoleDO> {

    PageResult<RoleDO> page(RoleQuery query);

    List<RoleDO> listAll();

    RoleDO create(RoleAO ao);

    RoleDO update(Long id, RoleAO ao);

    void delete(Long id);

    /**
     * 查询角色已授权的菜单 ID 列表。
     */
    List<Long> listMenuIds(Long roleId);

    /**
     * 覆盖设置角色的菜单授权。
     */
    void assignMenus(Long roleId, List<Long> menuIds);
}
