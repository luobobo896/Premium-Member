package com.premium.member.c.user.controller;

import com.premium.member.c.common.api.R;
import com.premium.member.c.framework.security.SecurityUtils;
import com.premium.member.c.user.dto.CUserInfoResponse;
import com.premium.member.c.user.service.CUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/c/user")
public class CUserController {

    private final CUserService userService;

    public CUserController(CUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/info")
    public R<CUserInfoResponse> info() {
        return R.ok(userService.info(SecurityUtils.currentUser().userId()));
    }
}
