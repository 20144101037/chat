package com.jin.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jin.chat.common.api.PageResult;
import com.jin.chat.domain.ao.RoomCreateAO;
import com.jin.chat.domain.ao.RoomUpdateAO;
import com.jin.chat.domain.entity.ChatRoomDO;
import com.jin.chat.domain.query.RoomQuery;
import com.jin.chat.domain.vo.RoomVO;

/**
 * <p>
 * 聊天室管理服务。
 * </p>
 *
 * @author jinshuai
 */
public interface ChatRoomService extends IService<ChatRoomDO> {

    /**
     * 创建聊天室（管理员）。
     */
    RoomVO createRoom(RoomCreateAO ao);

    /**
     * 修改聊天室属性。
     */
    RoomVO updateRoom(Long roomId, RoomUpdateAO ao);

    /**
     * 逻辑删除聊天室。
     */
    void deleteRoom(Long roomId);

    /**
     * 变更聊天室状态：ACTIVE / PAUSED / CLOSED。
     */
    void changeStatus(Long roomId, String status);

    /**
     * 分页查询聊天室列表（支持名称、状态过滤）。
     */
    PageResult<RoomVO> pageRooms(RoomQuery query);

    /**
     * 获取聊天室详情，并校验其可用（存在、未关闭）。
     */
    ChatRoomDO getAvailableRoom(Long roomId);

    /**
     * 转换为视图对象（含成员数与当前用户成员状态）。
     */
    RoomVO toVO(ChatRoomDO room);
}
