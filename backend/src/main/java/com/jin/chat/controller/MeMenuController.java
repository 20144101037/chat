package com.jin.chat.controller;

import com.jin.chat.common.api.ResultData;
import com.jin.chat.common.context.UserContextHolder;
import com.jin.chat.domain.vo.MenuVO;
import com.jin.chat.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 当前用户的可见菜单（根据其角色的菜单授权动态解析）。
 * </p>
 *
 * @author jinshuai
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeMenuController {

    private final MenuService menuService;

    @GetMapping("/menus")
    public ResultData<List<MenuVO>> menus() {
        return ResultData.success(menuService.listByUser(UserContextHolder.currentUserId()));
    }
}
