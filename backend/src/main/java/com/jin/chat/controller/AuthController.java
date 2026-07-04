package com.jin.chat.controller;

import com.jin.chat.common.api.ResultData;
import com.jin.chat.common.context.UserContextHolder;
import com.jin.chat.domain.ao.LoginAO;
import com.jin.chat.domain.ao.RegisterAO;
import com.jin.chat.domain.vo.LoginVO;
import com.jin.chat.domain.vo.UserVO;
import com.jin.chat.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 认证接口：注册、登录（无需鉴权）。
 * </p>
 *
 * @author jinshuai
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResultData<UserVO> register(@Valid @RequestBody RegisterAO ao) {
        return ResultData.success(authService.register(ao));
    }

    @PostMapping("/login")
    public ResultData<LoginVO> login(@Valid @RequestBody LoginAO ao) {
        return ResultData.success(authService.login(ao));
    }

    @PostMapping("/logout")
    public ResultData<Void> logout() {
        authService.logout(UserContextHolder.currentUserId());
        return ResultData.success(null);
    }
}
