package com.jin.chat.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jin.chat.domain.entity.RoleDO;
import com.jin.chat.domain.entity.UserDO;
import com.jin.chat.domain.entity.UserRoleDO;
import com.jin.chat.domain.enums.UserRoleEnum;
import com.jin.chat.mapper.RoleMapper;
import com.jin.chat.mapper.UserMapper;
import com.jin.chat.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * <p>
 * 启动时确保存在系统管理员账号：admin / admin123，并绑定 SYS_ADMIN 角色（RBAC）。
 * 使用真实 PasswordEncoder 加密，保证与登录校验一致。
 * </p>
 *
 * @author jinshuai
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_DEFAULT_PASSWORD = "admin123";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        UserDO admin = userMapper.selectByUsername(ADMIN_USERNAME);
        if (Objects.isNull(admin)) {
            admin = new UserDO();
            admin.setUsername(ADMIN_USERNAME);
            admin.setPasswordHash(passwordEncoder.encode(ADMIN_DEFAULT_PASSWORD));
            admin.setNickname("系统管理员");
            admin.setRole(UserRoleEnum.SYS_ADMIN.name());
            admin.setStatus("ACTIVE");
            userMapper.insert(admin);
            log.info("已初始化系统管理员账号: {} / {}", ADMIN_USERNAME, ADMIN_DEFAULT_PASSWORD);
        }
        // 确保管理员绑定 SYS_ADMIN 角色（兼容历史数据）
        bindSysAdminRole(admin.getId());
    }

    private void bindSysAdminRole(Long userId) {
        RoleDO role = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getRoleCode, UserRoleEnum.SYS_ADMIN.name()).last("limit 1"));
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
}
