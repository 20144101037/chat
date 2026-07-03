package com.jin.chat.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jin.chat.common.api.PageResult;
import com.jin.chat.domain.ao.BroadcastAO;
import com.jin.chat.domain.ao.MessageSubmitAO;
import com.jin.chat.domain.dto.WsMessage;
import com.jin.chat.domain.entity.MessageDO;
import com.jin.chat.domain.vo.MessageVO;

import java.util.List;

/**
 * <p>
 * 消息服务：用户提交、管理员广播、系统通知、历史查询。
 * </p>
 *
 * @author jinshuai
 */
public interface MessageService extends IService<MessageDO> {

    /**
     * 用户提交消息进入审核队列（状态 PENDING_REVIEW）。
     */
    MessageVO submit(Long roomId, MessageSubmitAO ao);

    /**
     * 管理员向多个聊天室广播消息（绕过审核，直接 APPROVED/NOTIFICATION 并推送）。
     */
    void broadcast(BroadcastAO ao);

    /**
     * 管理员向单个聊天室发送系统/紧急通知（绕过审核）。
     */
    void systemNotify(Long roomId, String content);

    /**
     * 查询房间已通过消息历史（按提交时间倒序分页游标）。
     */
    List<MessageVO> listApprovedHistory(Long roomId, Long beforeId, int size);

    /**
     * 分页查询当前用户提交的消息及审核状态。
     */
    PageResult<MessageVO> pageMyMessages(Long userId, String status, long pageNo, long pageSize);

    /**
     * 实体转视图对象（填充发送者昵称）。
     */
    MessageVO toVO(MessageDO message);

    /**
     * 实体转 WebSocket 协议对象。
     */
    WsMessage toWsMessage(MessageDO message, String type);
}
