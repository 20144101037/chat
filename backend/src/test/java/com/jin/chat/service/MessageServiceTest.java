package com.jin.chat.service;

import com.jin.chat.common.context.LoginUser;
import com.jin.chat.common.context.UserContextHolder;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.domain.ao.BroadcastAO;
import com.jin.chat.domain.ao.MessageSubmitAO;
import com.jin.chat.domain.entity.ChatRoomDO;
import com.jin.chat.domain.entity.MessageDO;
import com.jin.chat.domain.entity.UserDO;
import com.jin.chat.domain.enums.RoomStatusEnum;
import com.jin.chat.mapper.MessageMapper;
import com.jin.chat.mapper.UserMapper;
import com.jin.chat.repository.AuditQueueRepository;
import com.jin.chat.service.impl.MessageServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageMapper messageMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ChatRoomService chatRoomService;
    @Mock
    private MemberService memberService;
    @Mock
    private PushService pushService;
    @Mock
    private AuditQueueRepository auditQueueRepository;

    @Spy
    @InjectMocks
    private MessageServiceImpl messageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(messageService, "baseMapper", messageMapper);
        LoginUser user = new LoginUser();
        user.setUserId(5L);
        UserContextHolder.set(user);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void submit_shouldRejectPausedRoom() {
        ChatRoomDO room = new ChatRoomDO();
        room.setStatus(RoomStatusEnum.PAUSED.name());
        when(chatRoomService.getAvailableRoom(1L)).thenReturn(room);

        MessageSubmitAO ao = new MessageSubmitAO();
        ao.setContent("hello");
        assertThrows(BusinessException.class, () -> messageService.submit(1L, ao));
    }

    @Test
    void submit_shouldEnqueueAndPush() {
        ChatRoomDO room = new ChatRoomDO();
        room.setStatus(RoomStatusEnum.ACTIVE.name());
        when(chatRoomService.getAvailableRoom(1L)).thenReturn(room);
        doNothing().when(memberService).assertJoined(5L, 1L);
        doAnswer(inv -> {
            MessageDO msg = inv.getArgument(0);
            msg.setId(100L);
            return true;
        }).when(messageService).save(any(MessageDO.class));
        UserDO sender = new UserDO();
        sender.setId(5L);
        sender.setNickname("tester");
        when(userMapper.selectById(5L)).thenReturn(sender);

        MessageSubmitAO ao = new MessageSubmitAO();
        ao.setContent("hi");
        var vo = messageService.submit(1L, ao);

        assertEquals("hi", vo.getContent());
        verify(auditQueueRepository).enqueue(eq(100L), anyLong());
        verify(pushService).pushPendingToAdmins(any());
    }

    @Test
    void broadcast_shouldSaveAndPushEachRoom() {
        ChatRoomDO room = new ChatRoomDO();
        room.setId(2L);
        room.setStatus(RoomStatusEnum.ACTIVE.name());
        when(chatRoomService.getAvailableRoom(2L)).thenReturn(room);
        doAnswer(inv -> {
            MessageDO msg = inv.getArgument(0);
            msg.setId(200L);
            return true;
        }).when(messageService).save(any(MessageDO.class));
        when(userMapper.selectById(5L)).thenReturn(new UserDO());

        BroadcastAO ao = new BroadcastAO();
        ao.setRoomIds(List.of(2L));
        ao.setContent("notice");
        messageService.broadcast(ao);

        verify(pushService).pushToRoom(eq(2L), any());
    }

    @Test
    void listApprovedHistory_shouldCapSizeAt100() {
        when(messageMapper.listApprovedByRoom(eq(1L), isNull(), eq(100))).thenReturn(List.of());
        messageService.listApprovedHistory(1L, null, 500);
        verify(messageMapper).listApprovedByRoom(1L, null, 100);
    }
}
