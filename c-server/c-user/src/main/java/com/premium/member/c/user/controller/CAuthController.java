package com.premium.member.c.user.controller;

import java.util.Map;

import com.premium.member.c.common.api.R;
import com.premium.member.c.framework.security.RefreshCookieFactory;
import com.premium.member.c.user.dto.CUserInfoResponse;
import com.premium.member.c.user.dto.LoginRequest;
import com.premium.member.c.user.dto.LoginResult;
import com.premium.member.c.user.dto.RegisterRequest;
import com.premium.member.c.user.service.CUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/c/auth")
public class CAuthController {

    private final CUserService userService;
    private final RefreshCookieFactory cookieFactory;

    public CAuthController(CUserService userService, RefreshCookieFactory cookieFactory) {
        this.userService = userService;
        this.cookieFactory = cookieFactory;
    }

    @PostMapping("/register")
    public R<CUserInfoResponse> register(@Valid @RequestBody RegisterRequest request) {
        return R.ok(userService.register(request));
    }

    @PostMapping("/login")
    public R<LoginResult> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        CUserService.LoginSession session = userService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.create(session.refreshToken()).toString());
        return R.ok(new LoginResult(session.accessToken(), session.user()));
    }

    @PostMapping("/refresh")
    public R<Map<String, String>> refresh(HttpServletRequest request) {
        String accessToken = userService.refresh(cookieFactory.read(request));
        return R.ok(Map.of("accessToken", accessToken));
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        userService.logout(cookieFactory.read(request));
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString());
        return R.ok();
    }
}
