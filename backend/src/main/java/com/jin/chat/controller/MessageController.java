package com.jin.chat.controller;

import com.jin.chat.common.api.PageResult;
import com.jin.chat.common.api.ResultData;
import com.jin.chat.common.context.UserContextHolder;
import com.jin.chat.domain.ao.MessageSubmitAO;
import com.jin.chat.domain.vo.MessageVO;
import com.jin.chat.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 消息接口：提交消息、房间历史、我的消息。
 * </p>
 *
 * @author jinshuai
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 用户提交消息（进入审核队列）。
     */
    @PostMapping("/rooms/{roomId}/messages")
    public ResultData<MessageVO> submit(@PathVariable Long roomId,
                                        @Valid @RequestBody MessageSubmitAO ao) {
        return ResultData.success(messageService.submit(roomId, ao));
    }

    /**
     * 房间已通过消息历史（游标分页）。
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResultData<List<MessageVO>> history(@PathVariable Long roomId,
                                               @RequestParam(required = false) Long before,
                                               @RequestParam(defaultValue = "20") int size) {
        return ResultData.success(messageService.listApprovedHistory(roomId, before, size));
    }

    /**
     * 当前用户提交的消息与审核状态（分页）。
     */
    @GetMapping("/me/messages")
    public ResultData<PageResult<MessageVO>> myMessages(@RequestParam(required = false) String status,
                                                        @RequestParam(defaultValue = "1") long pageNo,
                                                        @RequestParam(defaultValue = "10") long pageSize) {
        return ResultData.success(
                messageService.pageMyMessages(UserContextHolder.currentUserId(), status, pageNo, pageSize));
    }
}
