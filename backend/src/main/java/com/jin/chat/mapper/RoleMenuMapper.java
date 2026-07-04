package com.jin.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jin.chat.domain.entity.RoleMenuDO;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 角色-菜单关联 Mapper。
 * </p>
 *
 * @author jinshuai
 */
@Repository
public interface RoleMenuMapper extends BaseMapper<RoleMenuDO> {
}
