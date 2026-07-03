package com.jin.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jin.chat.common.api.PageResult;
import com.jin.chat.common.context.LoginUser;
import com.jin.chat.common.context.UserContextHolder;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.common.exception.ErrorCodeEnum;
import com.jin.chat.domain.ao.RoomCreateAO;
import com.jin.chat.domain.ao.RoomUpdateAO;
import com.jin.chat.domain.entity.ChatRoomDO;
import com.jin.chat.domain.entity.UserChatRoomDO;
import com.jin.chat.domain.enums.JoinPolicyEnum;
import com.jin.chat.domain.enums.MemberStatusEnum;
import com.jin.chat.domain.enums.RoomStatusEnum;
import com.jin.chat.domain.query.RoomQuery;
import com.jin.chat.domain.vo.RoomVO;
import com.jin.chat.mapper.ChatRoomMapper;
import com.jin.chat.mapper.UserChatRoomMapper;
import com.jin.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * 聊天室管理服务实现。
 * </p>
 *
 * @author jinshuai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl extends ServiceImpl<ChatRoomMapper, ChatRoomDO> implements ChatRoomService {

    private final UserChatRoomMapper userChatRoomMapper;

    @Override
    public RoomVO createRoom(RoomCreateAO ao) {
        LoginUser current = UserContextHolder.require();
        ChatRoomDO room = new ChatRoomDO();
        room.setName(ao.getName());
        room.setDescription(ao.getDescription());
        room.setMaxUsers(ao.getMaxUsers());
        room.setJoinPolicy(Optional.ofNullable(ao.getJoinPolicy()).orElse(JoinPolicyEnum.OPEN.name()));
        room.setStatus(RoomStatusEnum.ACTIVE.name());
        room.setOwnerId(current.getUserId());
        save(room);
        return toVO(room);
    }

    @Override
    public RoomVO updateRoom(Long roomId, RoomUpdateAO ao) {
        ChatRoomDO room = getExistingRoom(roomId);
        if (StringUtils.hasText(ao.getName())) {
            room.setName(ao.getName());
        }
        if (ao.getDescription() != null) {
            room.setDescription(ao.getDescription());
        }
        if (ao.getMaxUsers() != null) {
            room.setMaxUsers(ao.getMaxUsers());
        }
        if (StringUtils.hasText(ao.getJoinPolicy())) {
            room.setJoinPolicy(ao.getJoinPolicy());
        }
        updateById(room);
        return toVO(room);
    }

    @Override
    public void deleteRoom(Long roomId) {
        getExistingRoom(roomId);
        removeById(roomId);
    }

    @Override
    public void changeStatus(Long roomId, String status) {
        // 校验状态合法
        RoomStatusEnum.valueOf(status);
        ChatRoomDO room = getExistingRoom(roomId);
        room.setStatus(status);
        updateById(room);
    }

    @Override
    public PageResult<RoomVO> pageRooms(RoomQuery query) {
        LambdaQueryWrapper<ChatRoomDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(ChatRoomDO::getName, query.getKeyword());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(ChatRoomDO::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(ChatRoomDO::getCreatedAt);
        Page<ChatRoomDO> page = page(new Page<>(query.getPageNo(), query.getPageSize()), wrapper);
        return PageResult.from(page, this::toVO);
    }

    @Override
    public ChatRoomDO getAvailableRoom(Long roomId) {
        ChatRoomDO room = getExistingRoom(roomId);
        if (RoomStatusEnum.CLOSED.name().equals(room.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.ROOM_CLOSED);
        }
        return room;
    }

    @Override
    public RoomVO toVO(ChatRoomDO room) {
        RoomVO vo = new RoomVO();
        vo.setId(room.getId());
        vo.setName(room.getName());
        vo.setDescription(room.getDescription());
        vo.setMaxUsers(room.getMaxUsers());
        vo.setJoinPolicy(room.getJoinPolicy());
        vo.setStatus(room.getStatus());
        vo.setOwnerId(room.getOwnerId());
        vo.setCreatedAt(room.getCreatedAt());
        vo.setMemberCount(userChatRoomMapper.countJoinedMembers(room.getId()));
        fillMyMemberStatus(vo, room.getId());
        return vo;
    }

    private void fillMyMemberStatus(RoomVO vo, Long roomId) {
        LoginUser current = UserContextHolder.get();
        if (current == null) {
            return;
        }
        UserChatRoomDO rel = userChatRoomMapper.selectOne(new LambdaQueryWrapper<UserChatRoomDO>()
                .eq(UserChatRoomDO::getUserId, current.getUserId())
                .eq(UserChatRoomDO::getRoomId, roomId)
                .last("limit 1"));
        if (rel != null && !MemberStatusEnum.LEFT.name().equals(rel.getMemberStatus())) {
            vo.setMyMemberStatus(rel.getMemberStatus());
        }
    }

    private ChatRoomDO getExistingRoom(Long roomId) {
        ChatRoomDO room = getById(roomId);
        if (Objects.isNull(room)) {
            throw new BusinessException(ErrorCodeEnum.ROOM_NOT_EXIST);
        }
        return room;
    }
}
