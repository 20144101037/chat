package com.jin.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jin.chat.common.context.LoginUser;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.common.exception.ErrorCodeEnum;
import com.jin.chat.common.util.JwtUtil;
import com.jin.chat.domain.ao.LoginAO;
import com.jin.chat.domain.ao.RegisterAO;
import com.jin.chat.domain.entity.RoleDO;
import com.jin.chat.domain.entity.UserDO;
import com.jin.chat.domain.entity.UserRoleDO;
import com.jin.chat.domain.enums.UserRoleEnum;
import com.jin.chat.domain.vo.LoginVO;
import com.jin.chat.domain.vo.UserVO;
import com.jin.chat.mapper.RoleMapper;
import com.jin.chat.mapper.UserMapper;
import com.jin.chat.mapper.UserRoleMapper;
import com.jin.chat.repository.LoggedInRepository;
import com.jin.chat.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * <p>
 * 认证服务实现。
 * </p>
 *
 * @author jinshuai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoggedInRepository loggedInRepository;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserVO register(RegisterAO ao) {
        UserDO exist = userMapper.selectByUsername(ao.getUsername());
        if (Objects.nonNull(exist)) {
            throw new BusinessException(ErrorCodeEnum.USERNAME_EXIST);
        }
        UserDO user = new UserDO();
        user.setUsername(ao.getUsername());
        user.setPasswordHash(passwordEncoder.encode(ao.getPassword()));
        user.setNickname(ao.getNickname() == null ? ao.getUsername() : ao.getNickname());
        user.setRole(UserRoleEnum.USER.name());
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        // 建立默认 USER 角色关联（RBAC）
        bindRole(user.getId(), UserRoleEnum.USER.name());
        return toVO(user);
    }

    /**
     * 为用户绑定指定角色（幂等）。
     */
    private void bindRole(Long userId, String roleCode) {
        RoleDO role = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getRoleCode, roleCode).last("limit 1"));
        if (role == null) {
            return;
        }
        Long count = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRoleDO>()
                .eq(UserRoleDO::getUserId, userId)
                .eq(UserRoleDO::getRoleId, role.getId()));
        if (count != null && count > 0) {
            return;
        }
        UserRoleDO ur = new UserRoleDO();
        ur.setUserId(userId);
        ur.setRoleId(role.getId());
        userRoleMapper.insert(ur);
    }

    @Override
    public LoginVO login(LoginAO ao) {
        UserDO user = userMapper.selectByUsername(ao.getUsername());
        if (Objects.isNull(user)) {
            throw new BusinessException(ErrorCodeEnum.ACCOUNT_NOT_EXIST);
        }
        if (!passwordEncoder.matches(ao.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCodeEnum.PASSWORD_ERROR);
        }
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRole(user.getRole());

        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.generateToken(loginUser));
        vo.setUser(toVO(user));
        loggedInRepository.markLoggedIn(user.getId());
        return vo;
    }

    @Override
    public void logout(Long userId) {
        if (userId != null) {
            loggedInRepository.markLoggedOut(userId);
        }
    }

    private UserVO toVO(UserDO user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
