package com.jin.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.common.exception.ErrorCodeEnum;
import com.jin.chat.domain.dto.WsMessage;
import com.jin.chat.domain.entity.ChatRoomDO;
import com.jin.chat.domain.entity.UserChatRoomDO;
import com.jin.chat.domain.enums.JoinPolicyEnum;
import com.jin.chat.domain.enums.MemberStatusEnum;
import com.jin.chat.domain.enums.MessageTypeEnum;
import com.jin.chat.domain.entity.UserDO;
import com.jin.chat.domain.vo.MemberCandidateVO;
import com.jin.chat.domain.vo.MemberVO;
import com.jin.chat.domain.vo.RoomVO;
import com.jin.chat.mapper.UserChatRoomMapper;
import com.jin.chat.mapper.UserMapper;
import com.jin.chat.service.ChatRoomService;
import com.jin.chat.service.MemberService;
import com.jin.chat.service.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * 成员服务实现。
 * </p>
 *
 * @author jinshuai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl extends ServiceImpl<UserChatRoomMapper, UserChatRoomDO> implements MemberService {

    private final UserChatRoomMapper userChatRoomMapper;
    private final UserMapper userMapper;
    private final PushService pushService;
    private final ChatRoomService chatRoomService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String join(Long roomId) {
        Long userId = com.jin.chat.common.context.UserContextHolder.currentUserId();
        ChatRoomDO room = chatRoomService.getAvailableRoom(roomId);

        // 人数上限校验
        Long joined = userChatRoomMapper.countJoinedMembers(roomId);
        if (room.getMaxUsers() != null && joined >= room.getMaxUsers()) {
            throw new BusinessException(ErrorCodeEnum.ROOM_FULL);
        }

        UserChatRoomDO rel = getRelation(userId, roomId);
        boolean approval = JoinPolicyEnum.APPROVAL.name().equals(room.getJoinPolicy());
        String targetStatus = approval ? MemberStatusEnum.PENDING.name() : MemberStatusEnum.JOINED.name();

        if (rel == null) {
            rel = new UserChatRoomDO();
            rel.setUserId(userId);
            rel.setRoomId(roomId);
            rel.setRoleInRoom("MEMBER");
            rel.setMemberStatus(targetStatus);
            rel.setJoinedAt(approval ? null : OffsetDateTime.now());
            save(rel);
        } else {
            if (MemberStatusEnum.JOINED.name().equals(rel.getMemberStatus())) {
                throw new BusinessException(ErrorCodeEnum.ALREADY_JOINED);
            }
            if (MemberStatusEnum.PENDING.name().equals(rel.getMemberStatus())) {
                throw new BusinessException(ErrorCodeEnum.JOIN_PENDING);
            }
            rel.setMemberStatus(targetStatus);
            rel.setJoinedAt(approval ? null : OffsetDateTime.now());
            updateById(rel);
        }
        return targetStatus;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void leave(Long roomId) {
        Long userId = com.jin.chat.common.context.UserContextHolder.currentUserId();
        UserChatRoomDO rel = getRelation(userId, roomId);
        if (rel == null || !MemberStatusEnum.JOINED.name().equals(rel.getMemberStatus())) {
            throw new BusinessException(ErrorCodeEnum.NOT_ROOM_MEMBER);
        }
        rel.setMemberStatus(MemberStatusEnum.LEFT.name());
        updateById(rel);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void approve(Long roomId, Long userId, boolean pass) {
        UserChatRoomDO rel = getRelation(userId, roomId);
        if (rel == null || !MemberStatusEnum.PENDING.name().equals(rel.getMemberStatus())) {
            throw new BusinessException(ErrorCodeEnum.PARAM_INVALID, "该用户无待审批的加入申请");
        }
        rel.setMemberStatus(pass ? MemberStatusEnum.JOINED.name() : MemberStatusEnum.REJECTED.name());
        rel.setJoinedAt(pass ? OffsetDateTime.now() : null);
        updateById(rel);

        pushService.notifyUser(userId, WsMessage.builder()
                .type(MessageTypeEnum.NOTIFICATION.name())
                .roomId(roomId)
                .content(pass ? "您的加入申请已通过" : "您的加入申请被拒绝")
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @Override
    public void assertJoined(Long userId, Long roomId) {
        if (!isJoined(userId, roomId)) {
            throw new BusinessException(ErrorCodeEnum.NOT_ROOM_MEMBER);
        }
    }

    @Override
    public boolean isJoined(Long userId, Long roomId) {
        UserChatRoomDO rel = getRelation(userId, roomId);
        return rel != null && MemberStatusEnum.JOINED.name().equals(rel.getMemberStatus());
    }

    @Override
    public List<RoomVO> listMyRooms(Long userId) {
        List<UserChatRoomDO> rels = list(new LambdaQueryWrapper<UserChatRoomDO>()
                .eq(UserChatRoomDO::getUserId, userId)
                .eq(UserChatRoomDO::getMemberStatus, MemberStatusEnum.JOINED.name()));
        if (rels.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roomIds = rels.stream().map(UserChatRoomDO::getRoomId).collect(Collectors.toList());
        return chatRoomService.listByIds(roomIds).stream()
                .map(chatRoomService::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MemberVO> listMembers(Long roomId, String status) {
        LambdaQueryWrapper<UserChatRoomDO> wrapper = new LambdaQueryWrapper<UserChatRoomDO>()
                .eq(UserChatRoomDO::getRoomId, roomId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(UserChatRoomDO::getMemberStatus, status);
        } else {
            wrapper.in(UserChatRoomDO::getMemberStatus,
                    MemberStatusEnum.JOINED.name(), MemberStatusEnum.PENDING.name());
        }
        wrapper.orderByDesc(UserChatRoomDO::getCreatedAt);
        List<UserChatRoomDO> rels = list(wrapper);
        if (rels.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = rels.stream().map(UserChatRoomDO::getUserId).distinct().collect(Collectors.toList());
        Map<Long, UserDO> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserDO::getId, u -> u));
        return rels.stream().map(rel -> {
            MemberVO vo = new MemberVO();
            vo.setUserId(rel.getUserId());
            vo.setMemberStatus(rel.getMemberStatus());
            vo.setRoleInRoom(rel.getRoleInRoom());
            vo.setJoinedAt(rel.getJoinedAt());
            UserDO user = userMap.get(rel.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void kickMember(Long roomId, Long userId) {
        ChatRoomDO room = chatRoomService.getById(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCodeEnum.ROOM_NOT_EXIST);
        }
        if (Objects.equals(room.getOwnerId(), userId)) {
            throw new BusinessException(ErrorCodeEnum.CANNOT_KICK_OWNER);
        }
        UserChatRoomDO rel = getRelation(userId, roomId);
        if (rel == null || (!MemberStatusEnum.JOINED.name().equals(rel.getMemberStatus())
                && !MemberStatusEnum.PENDING.name().equals(rel.getMemberStatus()))) {
            throw new BusinessException(ErrorCodeEnum.MEMBER_NOT_IN_ROOM);
        }
        rel.setMemberStatus(MemberStatusEnum.LEFT.name());
        updateById(rel);

        pushService.notifyUser(userId, WsMessage.builder()
                .type(MessageTypeEnum.NOTIFICATION.name())
                .roomId(roomId)
                .content("您已被管理员移出聊天室「" + room.getName() + "」")
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addMember(Long roomId, Long userId) {
        ChatRoomDO room = chatRoomService.getAvailableRoom(roomId);
        Long joined = userChatRoomMapper.countJoinedMembers(roomId);
        if (room.getMaxUsers() != null && joined >= room.getMaxUsers()) {
            throw new BusinessException(ErrorCodeEnum.ROOM_FULL);
        }
        UserDO user = userMapper.selectById(userId);
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCodeEnum.ACCOUNT_NOT_EXIST);
        }

        UserChatRoomDO rel = getRelation(userId, roomId);
        if (rel != null && MemberStatusEnum.JOINED.name().equals(rel.getMemberStatus())) {
            throw new BusinessException(ErrorCodeEnum.ALREADY_JOINED);
        }
        if (rel == null) {
            rel = new UserChatRoomDO();
            rel.setUserId(userId);
            rel.setRoomId(roomId);
            rel.setRoleInRoom("MEMBER");
            rel.setMemberStatus(MemberStatusEnum.JOINED.name());
            rel.setJoinedAt(OffsetDateTime.now());
            save(rel);
        } else {
            rel.setMemberStatus(MemberStatusEnum.JOINED.name());
            rel.setJoinedAt(OffsetDateTime.now());
            updateById(rel);
        }

        pushService.notifyUser(userId, WsMessage.builder()
                .type(MessageTypeEnum.NOTIFICATION.name())
                .roomId(roomId)
                .content("您已被管理员加入聊天室「" + room.getName() + "」")
                .timestamp(OffsetDateTime.now())
                .build());
    }

    @Override
    public List<MemberCandidateVO> searchCandidates(Long roomId, String keyword) {
        ChatRoomDO room = chatRoomService.getById(roomId);
        if (room == null) {
            throw new BusinessException(ErrorCodeEnum.ROOM_NOT_EXIST);
        }
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        List<UserDO> users = userMapper.selectList(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getStatus, "ACTIVE")
                .and(w -> w.like(UserDO::getUsername, keyword).or().like(UserDO::getNickname, keyword))
                .last("limit 20"));
        if (users.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = users.stream().map(UserDO::getId).collect(Collectors.toList());
        List<Long> excluded = list(new LambdaQueryWrapper<UserChatRoomDO>()
                .eq(UserChatRoomDO::getRoomId, roomId)
                .in(UserChatRoomDO::getUserId, userIds)
                .in(UserChatRoomDO::getMemberStatus,
                        MemberStatusEnum.JOINED.name(), MemberStatusEnum.PENDING.name()))
                .stream().map(UserChatRoomDO::getUserId).collect(Collectors.toList());

        return users.stream()
                .filter(u -> !excluded.contains(u.getId()))
                .map(u -> {
                    MemberCandidateVO vo = new MemberCandidateVO();
                    vo.setUserId(u.getId());
                    vo.setUsername(u.getUsername());
                    vo.setNickname(u.getNickname());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private UserChatRoomDO getRelation(Long userId, Long roomId) {
        return userChatRoomMapper.selectOne(new LambdaQueryWrapper<UserChatRoomDO>()
                .eq(UserChatRoomDO::getUserId, userId)
                .eq(UserChatRoomDO::getRoomId, roomId)
                .last("limit 1"));
    }
}
