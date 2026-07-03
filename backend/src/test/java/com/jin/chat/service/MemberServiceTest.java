package com.jin.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jin.chat.common.context.LoginUser;
import com.jin.chat.common.context.UserContextHolder;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.domain.dto.WsMessage;
import com.jin.chat.domain.entity.ChatRoomDO;
import com.jin.chat.domain.entity.UserChatRoomDO;
import com.jin.chat.domain.enums.JoinPolicyEnum;
import com.jin.chat.mapper.UserChatRoomMapper;
import com.jin.chat.mapper.UserMapper;
import com.jin.chat.service.impl.MemberServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private UserChatRoomMapper userChatRoomMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PushService pushService;
    @Mock
    private ChatRoomService chatRoomService;

    @Spy
    @InjectMocks
    private MemberServiceImpl memberService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(memberService, "baseMapper", userChatRoomMapper);
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        UserContextHolder.set(user);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void join_openPolicy_shouldJoinImmediately() {
        ChatRoomDO room = room(JoinPolicyEnum.OPEN.name(), 100);
        when(chatRoomService.getAvailableRoom(10L)).thenReturn(room);
        when(userChatRoomMapper.countJoinedMembers(10L)).thenReturn(0L);
        when(userChatRoomMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doReturn(true).when(memberService).save(any(UserChatRoomDO.class));

        assertEquals("JOINED", memberService.join(10L));
    }

    @Test
    void join_approvalPolicy_shouldPending() {
        ChatRoomDO room = room(JoinPolicyEnum.APPROVAL.name(), 100);
        when(chatRoomService.getAvailableRoom(10L)).thenReturn(room);
        when(userChatRoomMapper.countJoinedMembers(10L)).thenReturn(0L);
        when(userChatRoomMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doReturn(true).when(memberService).save(any(UserChatRoomDO.class));

        assertEquals("PENDING", memberService.join(10L));
    }

    @Test
    void join_shouldReject_whenRoomFull() {
        ChatRoomDO room = room(JoinPolicyEnum.OPEN.name(), 1);
        when(chatRoomService.getAvailableRoom(10L)).thenReturn(room);
        when(userChatRoomMapper.countJoinedMembers(10L)).thenReturn(1L);
        assertThrows(BusinessException.class, () -> memberService.join(10L));
    }

    @Test
    void join_shouldReject_whenAlreadyJoined() {
        ChatRoomDO room = room(JoinPolicyEnum.OPEN.name(), 10);
        when(chatRoomService.getAvailableRoom(10L)).thenReturn(room);
        when(userChatRoomMapper.countJoinedMembers(10L)).thenReturn(1L);
        UserChatRoomDO rel = new UserChatRoomDO();
        rel.setMemberStatus("JOINED");
        when(userChatRoomMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(rel);
        assertThrows(BusinessException.class, () -> memberService.join(10L));
    }

    @Test
    void leave_shouldMarkLeft() {
        UserChatRoomDO rel = new UserChatRoomDO();
        rel.setMemberStatus("JOINED");
        when(userChatRoomMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(rel);
        doReturn(true).when(memberService).updateById(any(UserChatRoomDO.class));

        memberService.leave(10L);
        assertEquals("LEFT", rel.getMemberStatus());
    }

    @Test
    void approve_shouldNotifyUser() {
        UserChatRoomDO rel = new UserChatRoomDO();
        rel.setMemberStatus("PENDING");
        when(userChatRoomMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(rel);
        doReturn(true).when(memberService).updateById(any(UserChatRoomDO.class));

        memberService.approve(10L, 2L, true);

        assertEquals("JOINED", rel.getMemberStatus());
        verify(pushService).notifyUser(eq(2L), any(WsMessage.class));
    }

    @Test
    void isJoined_shouldReflectStatus() {
        UserChatRoomDO rel = new UserChatRoomDO();
        rel.setMemberStatus("JOINED");
        when(userChatRoomMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(rel);
        assertTrue(memberService.isJoined(1L, 10L));
    }

    private static ChatRoomDO room(String policy, int maxUsers) {
        ChatRoomDO room = new ChatRoomDO();
        room.setId(10L);
        room.setJoinPolicy(policy);
        room.setMaxUsers(maxUsers);
        return room;
    }
}
