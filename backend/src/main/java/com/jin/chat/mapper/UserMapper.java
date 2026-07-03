package com.jin.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jin.chat.domain.entity.UserDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 用户 Mapper。
 * </p>
 *
 * @author jinshuai
 */
@Repository
public interface UserMapper extends BaseMapper<UserDO> {

    /**
     * 根据用户名查询（含逻辑删除过滤）。
     */
    UserDO selectByUsername(@Param("username") String username);
}
