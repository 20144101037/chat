package com.jin.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jin.chat.domain.entity.UserChatRoomDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 用户-聊天室关联 Mapper。
 * </p>
 *
 * @author jinshuai
 */
@Repository
public interface UserChatRoomMapper extends BaseMapper<UserChatRoomDO> {

    /**
     * 统计房间已加入成员数。
     */
    Long countJoinedMembers(@Param("roomId") Long roomId);
}
