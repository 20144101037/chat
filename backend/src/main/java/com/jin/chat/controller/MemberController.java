package com.jin.chat.controller;

import com.jin.chat.common.api.ResultData;
import com.jin.chat.common.context.UserContextHolder;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.common.exception.ErrorCodeEnum;
import com.jin.chat.domain.vo.MemberCandidateVO;
import com.jin.chat.domain.vo.MemberVO;
import com.jin.chat.domain.vo.RoomVO;
import com.jin.chat.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 聊天室成员接口：加入、退出、审批、我的聊天室。
 * </p>
 *
 * @author jinshuai
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/{id}/join")
    public ResultData<Map<String, String>> join(@PathVariable Long id) {
        String status = memberService.join(id);
        return ResultData.success(Map.of("memberStatus", status));
    }

    @PostMapping("/{id}/leave")
    public ResultData<Void> leave(@PathVariable Long id) {
        memberService.leave(id);
        return ResultData.success();
    }

    @GetMapping("/{id}/members")
    public ResultData<List<MemberVO>> members(@PathVariable Long id,
                                              @RequestParam(required = false) String status) {
        assertAdmin();
        return ResultData.success(memberService.listMembers(id, status));
    }

    @PostMapping("/{id}/members/{userId}/approve")
    public ResultData<Void> approve(@PathVariable Long id,
                                    @PathVariable Long userId,
                                    @RequestBody Map<String, Boolean> body) {
        assertAdmin();
        memberService.approve(id, userId, Boolean.TRUE.equals(body.get("pass")));
        return ResultData.success();
    }

    @PostMapping("/{id}/members/{userId}/add")
    public ResultData<Void> addMember(@PathVariable Long id, @PathVariable Long userId) {
        assertAdmin();
        memberService.addMember(id, userId);
        return ResultData.success();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResultData<Void> kickMember(@PathVariable Long id, @PathVariable Long userId) {
        assertAdmin();
        memberService.kickMember(id, userId);
        return ResultData.success();
    }

    @GetMapping("/{id}/member-candidates")
    public ResultData<List<MemberCandidateVO>> candidates(@PathVariable Long id,
                                                          @RequestParam String keyword) {
        assertAdmin();
        return ResultData.success(memberService.searchCandidates(id, keyword));
    }

    @GetMapping("/mine")
    public ResultData<List<RoomVO>> myRooms() {
        return ResultData.success(memberService.listMyRooms(UserContextHolder.currentUserId()));
    }

    private void assertAdmin() {
        if (!UserContextHolder.require().isAdmin()) {
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN);
        }
    }
}
