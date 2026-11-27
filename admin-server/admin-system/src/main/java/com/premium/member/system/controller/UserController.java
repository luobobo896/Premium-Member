package com.premium.member.system.controller;

import com.premium.member.common.api.R;
import com.premium.member.framework.security.SecurityUtils;
import com.premium.member.system.dto.UserInfoResponse;
import com.premium.member.system.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 返回当前登录用户信息。
     *
     * @return 用户信息
     */
    @GetMapping("/info")
    public R<UserInfoResponse> info() {
        return R.ok(authService.userInfo(SecurityUtils.currentUser().userId()));
    }
}
