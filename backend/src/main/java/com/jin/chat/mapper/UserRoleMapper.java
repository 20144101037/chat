package com.jin.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jin.chat.domain.entity.UserRoleDO;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 用户-角色关联 Mapper。
 * </p>
 *
 * @author jinshuai
 */
@Repository
public interface UserRoleMapper extends BaseMapper<UserRoleDO> {
}
