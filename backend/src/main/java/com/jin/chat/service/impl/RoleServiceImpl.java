package com.jin.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jin.chat.common.api.PageResult;
import com.jin.chat.common.cache.PermissionCache;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.common.exception.ErrorCodeEnum;
import com.jin.chat.domain.ao.RoleAO;
import com.jin.chat.domain.entity.RoleDO;
import com.jin.chat.domain.entity.RoleMenuDO;
import com.jin.chat.domain.entity.UserRoleDO;
import com.jin.chat.domain.query.RoleQuery;
import com.jin.chat.mapper.RoleMapper;
import com.jin.chat.mapper.RoleMenuMapper;
import com.jin.chat.mapper.UserRoleMapper;
import com.jin.chat.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色服务实现。
 * </p>
 *
 * @author jinshuai
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, RoleDO> implements RoleService {

    private final RoleMenuMapper roleMenuMapper;
    private final UserRoleMapper userRoleMapper;
    private final PermissionCache permissionCache;

    @Override
    public PageResult<RoleDO> page(RoleQuery query) {
        LambdaQueryWrapper<RoleDO> wrapper = new LambdaQueryWrapper<RoleDO>()
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(RoleDO::getRoleCode, query.getKeyword())
                        .or().like(RoleDO::getRoleName, query.getKeyword()))
                .orderByAsc(RoleDO::getId);
        Page<RoleDO> page = page(new Page<>(query.getPageNo(), query.getPageSize()), wrapper);
        return PageResult.from(page, r -> r);
    }

    @Override
    public List<RoleDO> listAll() {
        return list(new LambdaQueryWrapper<RoleDO>().orderByAsc(RoleDO::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public RoleDO create(RoleAO ao) {
        if (existsCode(ao.getRoleCode(), null)) {
            throw new BusinessException(ErrorCodeEnum.ROLE_CODE_EXIST);
        }
        RoleDO role = new RoleDO();
        role.setRoleCode(ao.getRoleCode());
        role.setRoleName(ao.getRoleName());
        role.setDescription(ao.getDescription());
        role.setBuiltIn(Boolean.FALSE);
        save(role);
        return role;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public RoleDO update(Long id, RoleAO ao) {
        RoleDO role = getById(id);
        if (role == null) {
            throw new BusinessException(ErrorCodeEnum.ROLE_NOT_EXIST);
        }
        // 内置角色不允许修改编码
        if (Boolean.TRUE.equals(role.getBuiltIn()) && !role.getRoleCode().equals(ao.getRoleCode())) {
            throw new BusinessException(ErrorCodeEnum.ROLE_BUILT_IN);
        }
        if (existsCode(ao.getRoleCode(), id)) {
            throw new BusinessException(ErrorCodeEnum.ROLE_CODE_EXIST);
        }
        role.setRoleCode(ao.getRoleCode());
        role.setRoleName(ao.getRoleName());
        role.setDescription(ao.getDescription());
        updateById(role);
        // 角色编码变更可能影响 SYS_ADMIN 判定与菜单解析，失效全部权限缓存
        permissionCache.evictAll();
        return role;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long id) {
        RoleDO role = getById(id);
        if (role == null) {
            throw new BusinessException(ErrorCodeEnum.ROLE_NOT_EXIST);
        }
        if (Boolean.TRUE.equals(role.getBuiltIn())) {
            throw new BusinessException(ErrorCodeEnum.ROLE_BUILT_IN);
        }
        removeById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenuDO>().eq(RoleMenuDO::getRoleId, id));
        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleDO>().eq(UserRoleDO::getRoleId, id));
        permissionCache.evictAll();
    }

    @Override
    public List<Long> listMenuIds(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<RoleMenuDO>()
                        .eq(RoleMenuDO::getRoleId, roleId))
                .stream().map(RoleMenuDO::getMenuId).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void assignMenus(Long roleId, List<Long> menuIds) {
        RoleDO role = getById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCodeEnum.ROLE_NOT_EXIST);
        }
        roleMenuMapper.delete(new LambdaQueryWrapper<RoleMenuDO>().eq(RoleMenuDO::getRoleId, roleId));
        List<Long> ids = menuIds == null ? Collections.emptyList() : menuIds;
        for (Long menuId : ids) {
            RoleMenuDO rm = new RoleMenuDO();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
        // 该角色下所有用户的可见菜单都会变化
        permissionCache.evictAll();
    }

    private boolean existsCode(String code, Long excludeId) {
        return count(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getRoleCode, code)
                .ne(excludeId != null, RoleDO::getId, excludeId)) > 0;
    }
}
