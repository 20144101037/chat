package com.jin.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jin.chat.common.api.PageResult;
import com.jin.chat.common.cache.PermissionCache;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.common.exception.ErrorCodeEnum;
import com.jin.chat.common.util.TransactionUtils;
import com.jin.chat.domain.entity.RoleDO;
import com.jin.chat.domain.entity.UserDO;
import com.jin.chat.domain.entity.UserRoleDO;
import com.jin.chat.domain.enums.UserRoleEnum;
import com.jin.chat.domain.query.UserQuery;
import com.jin.chat.domain.vo.AdminUserVO;
import com.jin.chat.mapper.RoleMapper;
import com.jin.chat.mapper.UserMapper;
import com.jin.chat.mapper.UserRoleMapper;
import com.jin.chat.repository.LoggedInRepository;
import com.jin.chat.repository.SessionRepository;
import com.jin.chat.repository.TokenValidityRepository;
import com.jin.chat.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户管理服务实现。
 * </p>
 *
 * @author jinshuai
 */
@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    /** 角色权限优先级，用于计算主角色 */
    private static final Map<String, Integer> ROLE_PRIORITY = Map.of(
            UserRoleEnum.USER.name(), 1,
            UserRoleEnum.ROOM_ADMIN.name(), 2,
            UserRoleEnum.SYS_ADMIN.name(), 3);

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PermissionCache permissionCache;
    private final PasswordEncoder passwordEncoder;
    private final TokenValidityRepository tokenValidityRepository;
    private final SessionRepository sessionRepository;
    private final LoggedInRepository loggedInRepository;

    @Override
    public PageResult<AdminUserVO> page(UserQuery query) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<UserDO>()
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(UserDO::getUsername, query.getKeyword())
                        .or().like(UserDO::getNickname, query.getKeyword()))
                .eq(StringUtils.hasText(query.getStatus()), UserDO::getStatus, query.getStatus())
                .orderByAsc(UserDO::getId);
        Page<UserDO> page = userMapper.selectPage(new Page<>(query.getPageNo(), query.getPageSize()), wrapper);

        List<Long> userIds = page.getRecords().stream().map(UserDO::getId).collect(Collectors.toList());
        Map<Long, List<UserRoleDO>> userRoleMap = userIds.isEmpty() ? Collections.emptyMap()
                : userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleDO>()
                        .in(UserRoleDO::getUserId, userIds))
                .stream().collect(Collectors.groupingBy(UserRoleDO::getUserId));
        Map<Long, RoleDO> roleMap = allRolesById();

        List<AdminUserVO> records = page.getRecords().stream().map(u -> {
            AdminUserVO vo = new AdminUserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setNickname(u.getNickname());
            vo.setRole(u.getRole());
            vo.setStatus(u.getStatus());
            List<UserRoleDO> rels = userRoleMap.getOrDefault(u.getId(), Collections.emptyList());
            vo.setRoleIds(rels.stream().map(UserRoleDO::getRoleId).collect(Collectors.toList()));
            vo.setRoleNames(rels.stream()
                    .map(r -> roleMap.get(r.getRoleId()))
                    .filter(java.util.Objects::nonNull)
                    .map(RoleDO::getRoleName)
                    .collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public List<Long> listRoleIds(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleDO>()
                        .eq(UserRoleDO::getUserId, userId))
                .stream().map(UserRoleDO::getRoleId).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void assignRoles(Long userId, List<Long> roleIds) {
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.ACCOUNT_NOT_EXIST);
        }
        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleDO>().eq(UserRoleDO::getUserId, userId));
        List<Long> ids = roleIds == null ? Collections.emptyList() : roleIds;
        for (Long roleId : ids) {
            UserRoleDO ur = new UserRoleDO();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
        // 同步主角色为最高权限角色（USER 兜底）
        String primaryRole = resolvePrimaryRole(ids);
        user.setRole(primaryRole);
        userMapper.updateById(user);
        // 失效该用户的角色/菜单缓存
        permissionCache.evictUser(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateStatus(Long userId, String status) {
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.ACCOUNT_NOT_EXIST);
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void resetPassword(Long userId, String rawPassword) {
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.ACCOUNT_NOT_EXIST);
        }
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        userMapper.updateById(user);
        // 事务提交后使旧令牌失效并清理登录/WebSocket 状态，实现强制下线
        long now = System.currentTimeMillis();
        TransactionUtils.afterCommit(() -> {
            tokenValidityRepository.invalidateBefore(userId, now);
            loggedInRepository.markLoggedOut(userId);
            sessionRepository.offline(userId);
        });
    }

    private String resolvePrimaryRole(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return UserRoleEnum.USER.name();
        }
        Map<Long, RoleDO> roleMap = allRolesById();
        return roleIds.stream()
                .map(roleMap::get)
                .filter(java.util.Objects::nonNull)
                .map(RoleDO::getRoleCode)
                .max((a, b) -> ROLE_PRIORITY.getOrDefault(a, 0) - ROLE_PRIORITY.getOrDefault(b, 0))
                .orElse(UserRoleEnum.USER.name());
    }

    private Map<Long, RoleDO> allRolesById() {
        return roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(RoleDO::getId, Function.identity()));
    }
}
