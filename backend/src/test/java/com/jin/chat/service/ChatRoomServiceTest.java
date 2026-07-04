package com.jin.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jin.chat.common.constant.SysConfigKeys;
import com.jin.chat.common.context.LoginUser;
import com.jin.chat.common.context.UserContextHolder;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.domain.ao.RoomCreateAO;
import com.jin.chat.domain.ao.RoomUpdateAO;
import com.jin.chat.domain.entity.ChatRoomDO;
import com.jin.chat.domain.entity.UserChatRoomDO;
import com.jin.chat.domain.enums.RoomStatusEnum;
import com.jin.chat.mapper.ChatRoomMapper;
import com.jin.chat.mapper.UserChatRoomMapper;
import com.jin.chat.service.impl.ChatRoomServiceImpl;
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
class ChatRoomServiceTest {

    @Mock
    private ChatRoomMapper chatRoomMapper;
    @Mock
    private UserChatRoomMapper userChatRoomMapper;
    @Mock
    private SysConfigService sysConfigService;

    @Spy
    @InjectMocks
    private ChatRoomServiceImpl chatRoomService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chatRoomService, "baseMapper", chatRoomMapper);
        LoginUser user = new LoginUser();
        user.setUserId(100L);
        UserContextHolder.set(user);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void createRoom_shouldUseConfigDefaultMaxUsers_whenNotProvided() {
        when(sysConfigService.getInt(SysConfigKeys.ROOM_DEFAULT_MAX_USERS, SysConfigKeys.DEFAULT_ROOM_MAX_USERS))
                .thenReturn(100);
        RoomCreateAO ao = new RoomCreateAO();
        ao.setName("配置房");
        doAnswer(inv -> {
            ChatRoomDO room = inv.getArgument(0);
            room.setId(2L);
            return true;
        }).when(chatRoomService).save(any(ChatRoomDO.class));
        when(userChatRoomMapper.countJoinedMembers(2L)).thenReturn(0L);

        var vo = chatRoomService.createRoom(ao);
        assertEquals(100, vo.getMaxUsers());
    }

    @Test
    void createRoom_shouldUseCurrentUserAsOwner() {
        RoomCreateAO ao = new RoomCreateAO();
        ao.setName("测试房");
        ao.setMaxUsers(50);
        doAnswer(inv -> {
            ChatRoomDO room = inv.getArgument(0);
            room.setId(1L);
            return true;
        }).when(chatRoomService).save(any(ChatRoomDO.class));
        when(userChatRoomMapper.countJoinedMembers(1L)).thenReturn(0L);

        var vo = chatRoomService.createRoom(ao);
        assertEquals("测试房", vo.getName());
        assertEquals(100L, vo.getOwnerId());
        assertEquals(RoomStatusEnum.ACTIVE.name(), vo.getStatus());
    }

    @Test
    void getAvailableRoom_shouldRejectClosedRoom() {
        ChatRoomDO room = new ChatRoomDO();
        room.setId(2L);
        room.setStatus(RoomStatusEnum.CLOSED.name());
        doReturn(room).when(chatRoomService).getById(2L);
        assertThrows(BusinessException.class, () -> chatRoomService.getAvailableRoom(2L));
    }

    @Test
    void getAvailableRoom_shouldThrow_whenMissing() {
        doReturn(null).when(chatRoomService).getById(99L);
        assertThrows(BusinessException.class, () -> chatRoomService.getAvailableRoom(99L));
    }

    @Test
    void updateRoom_shouldApplyPartialFields() {
        ChatRoomDO room = new ChatRoomDO();
        room.setId(3L);
        room.setName("old");
        doReturn(room).when(chatRoomService).getById(3L);
        doReturn(true).when(chatRoomService).updateById(any(ChatRoomDO.class));
        when(userChatRoomMapper.countJoinedMembers(3L)).thenReturn(1L);

        RoomUpdateAO ao = new RoomUpdateAO();
        ao.setName("new");
        var vo = chatRoomService.updateRoom(3L, ao);
        assertEquals("new", vo.getName());
    }

    @Test
    void changeStatus_shouldValidateEnum() {
        assertThrows(IllegalArgumentException.class, () -> chatRoomService.changeStatus(4L, "INVALID"));
    }

    @Test
    void toVO_shouldFillMemberStatus_whenJoined() {
        ChatRoomDO room = new ChatRoomDO();
        room.setId(5L);
        room.setName("r");
        when(userChatRoomMapper.countJoinedMembers(5L)).thenReturn(2L);
        UserChatRoomDO rel = new UserChatRoomDO();
        rel.setMemberStatus("JOINED");
        when(userChatRoomMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(rel);

        var vo = chatRoomService.toVO(room);
        assertEquals(2, vo.getMemberCount());
        assertEquals("JOINED", vo.getMyMemberStatus());
    }
}
