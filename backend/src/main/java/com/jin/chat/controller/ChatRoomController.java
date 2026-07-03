package com.jin.chat.controller;

import com.jin.chat.common.api.PageResult;
import com.jin.chat.common.api.ResultData;
import com.jin.chat.common.context.UserContextHolder;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.common.exception.ErrorCodeEnum;
import com.jin.chat.domain.ao.BroadcastAO;
import com.jin.chat.domain.ao.RoomCreateAO;
import com.jin.chat.domain.ao.RoomUpdateAO;
import com.jin.chat.domain.query.RoomQuery;
import com.jin.chat.domain.vo.RoomVO;
import com.jin.chat.service.ChatRoomService;
import com.jin.chat.service.MenuPermissionService;
import com.jin.chat.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * <p>
 * 聊天室管理接口。创建/修改/删除/状态变更/广播需管理员权限。
 * </p>
 *
 * @author jinshuai
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private static final String MENU_BROADCAST = "/app/broadcast";

    private final ChatRoomService chatRoomService;
    private final MessageService messageService;
    private final MenuPermissionService menuPermissionService;

    @PostMapping
    public ResultData<RoomVO> create(@Valid @RequestBody RoomCreateAO ao) {
        assertAdmin();
        return ResultData.success(chatRoomService.createRoom(ao));
    }

    @PutMapping("/{id}")
    public ResultData<RoomVO> update(@PathVariable Long id, @Valid @RequestBody RoomUpdateAO ao) {
        assertAdmin();
        return ResultData.success(chatRoomService.updateRoom(id, ao));
    }

    @DeleteMapping("/{id}")
    public ResultData<Void> delete(@PathVariable Long id) {
        assertAdmin();
        chatRoomService.deleteRoom(id);
        return ResultData.success();
    }

    @PatchMapping("/{id}/status")
    public ResultData<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        assertAdmin();
        chatRoomService.changeStatus(id, body.get("status"));
        return ResultData.success();
    }

    @GetMapping
    public ResultData<PageResult<RoomVO>> page(RoomQuery query) {
        return ResultData.success(chatRoomService.pageRooms(query));
    }

    @GetMapping("/{id}")
    public ResultData<RoomVO> detail(@PathVariable Long id) {
        return ResultData.success(chatRoomService.toVO(chatRoomService.getAvailableRoom(id)));
    }

    /**
     * 管理员向多个聊天室广播消息（绕过审核）。
     */
    @PostMapping("/broadcast")
    public ResultData<Void> broadcast(@Valid @RequestBody BroadcastAO ao) {
        menuPermissionService.requireMenuPath(MENU_BROADCAST);
        messageService.broadcast(ao);
        return ResultData.success();
    }

    /**
     * 紧急/系统通知（绕过审核）。
     */
    @PostMapping("/{id}/system-notify")
    public ResultData<Void> systemNotify(@PathVariable Long id, @RequestBody Map<String, String> body) {
        assertAdmin();
        messageService.systemNotify(id, body.get("content"));
        return ResultData.success();
    }

    private void assertAdmin() {
        if (!UserContextHolder.require().isAdmin()) {
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN);
        }
    }
}
