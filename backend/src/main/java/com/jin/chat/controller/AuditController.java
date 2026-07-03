package com.jin.chat.controller;

import com.jin.chat.common.api.PageResult;
import com.jin.chat.common.api.ResultData;
import com.jin.chat.common.context.UserContextHolder;
import com.jin.chat.domain.ao.AuditAO;
import com.jin.chat.domain.ao.BatchAuditAO;
import com.jin.chat.domain.vo.MessageVO;
import com.jin.chat.service.AuditService;
import com.jin.chat.service.MenuPermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 审核接口（仅管理员）：待审核列表、通过、拒绝、批量审核。
 * </p>
 *
 * @author jinshuai
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private static final String MENU_AUDIT = "/app/audit";

    private final AuditService auditService;
    private final MenuPermissionService menuPermissionService;

    @GetMapping("/pending")
    public ResultData<PageResult<MessageVO>> pending(@RequestParam(required = false) Long roomId,
                                                     @RequestParam(defaultValue = "1") long pageNo,
                                                     @RequestParam(defaultValue = "10") long pageSize) {
        assertAdmin();
        return ResultData.success(auditService.listPending(roomId, pageNo, pageSize));
    }

    @PostMapping("/{messageId}/approve")
    public ResultData<Void> approve(@PathVariable Long messageId) {
        Long reviewerId = requireAdminId();
        auditService.approve(messageId, reviewerId);
        return ResultData.success();
    }

    @PostMapping("/{messageId}/reject")
    public ResultData<Void> reject(@PathVariable Long messageId, @RequestBody(required = false) AuditAO ao) {
        Long reviewerId = requireAdminId();
        auditService.reject(messageId, reviewerId, ao == null ? null : ao.getReason());
        return ResultData.success();
    }

    @PostMapping("/batch")
    public ResultData<Void> batch(@Valid @RequestBody BatchAuditAO ao) {
        Long reviewerId = requireAdminId();
        auditService.batchAudit(ao, reviewerId);
        return ResultData.success();
    }

    private Long requireAdminId() {
        assertAdmin();
        return UserContextHolder.currentUserId();
    }

    private void assertAdmin() {
        menuPermissionService.requireMenuPath(MENU_AUDIT);
    }
}
