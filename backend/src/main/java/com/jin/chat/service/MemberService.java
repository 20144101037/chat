package com.jin.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jin.chat.domain.entity.UserChatRoomDO;
import com.jin.chat.domain.vo.MemberVO;
import com.jin.chat.domain.vo.RoomVO;

import java.util.List;

/**
 * <p>
 * 聊天室成员服务：加入、退出、审批、成员校验。
 * </p>
 *
 * @author jinshuai
 */
public interface MemberService extends IService<UserChatRoomDO> {

    /**
     * 申请加入聊天室。开放房间直接加入，审批房间进入 PENDING。
     *
     * @return 成员状态：JOINED / PENDING
     */
    String join(Long roomId);

    /**
     * 退出聊天室。
     */
    void leave(Long roomId);

    /**
     * 管理员审批加入申请。
     */
    void approve(Long roomId, Long userId, boolean pass);

    /**
     * 校验用户是否为房间已加入成员，否则抛异常。
     */
    void assertJoined(Long userId, Long roomId);

    /**
     * 判断用户是否已加入房间。
     */
    boolean isJoined(Long userId, Long roomId);

    /**
     * 查询当前用户已加入的聊天室列表。
     */
    List<RoomVO> listMyRooms(Long userId);

    /**
     * 查询聊天室成员列表（管理员用）。
     *
     * @param roomId 聊天室 ID
     * @param status 成员状态过滤，为空则查询 JOINED 与 PENDING
     */
    List<MemberVO> listMembers(Long roomId, String status);
}
