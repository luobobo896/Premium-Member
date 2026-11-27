package com.premium.member.system.controller;

import java.util.List;

import com.premium.member.common.api.R;
import com.premium.member.framework.security.CookieFactory;
import com.premium.member.framework.security.SecurityUtils;
import com.premium.member.system.dto.CaptchaResponse;
import com.premium.member.system.dto.LoginRequest;
import com.premium.member.system.dto.LoginResponse;
import com.premium.member.system.dto.LoginResult;
import com.premium.member.system.service.AuthService;
import com.premium.member.system.service.CaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final CookieFactory cookieFactory;

    public AuthController(AuthService authService, CaptchaService captchaService, CookieFactory cookieFactory) {
        this.authService = authService;
        this.captchaService = captchaService;
        this.cookieFactory = cookieFactory;
    }

    /**
     * 登录并写入 refresh token Cookie。
     *
     * @param request 登录参数
     * @return 统一响应
     */
    @PostMapping("/login")
    public ResponseEntity<R<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.create(result.refreshToken()).toString())
                .body(R.ok(result.body()));
    }

    /**
     * 刷新 access token，返回裸字符串以匹配 vben baseRequestClient 的读取方式。
     *
     * @param request HTTP 请求，用于读取 jwt Cookie
     * @return access token 字符串
     */
    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(HttpServletRequest request) {
        String refreshToken = cookieFactory.read(request);
        String accessToken = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.SET_COOKIE, cookieFactory.create(refreshToken).toString())
                .body(accessToken);
    }

    /**
     * 退出登录并清除 Cookie。
     *
     * @param request           HTTP 请求
     * @param authorizationHeader 可选 access token
     * @return 空数据响应
     */
    @PostMapping("/logout")
    public ResponseEntity<R<String>> logout(HttpServletRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        authService.logout(cookieFactory.read(request), authorizationHeader);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                .body(R.ok(""));
    }

    /**
     * 返回当前用户权限码，前端按钮权限使用。
     *
     * @return 权限码列表
     */
    @GetMapping("/codes")
    public R<List<String>> codes() {
        return R.ok(authService.accessCodes(SecurityUtils.currentUser().userId()));
    }

    /**
     * 生成服务端验证码；vben 默认使用前端滑块验证，不传 captcha 字段时登录不做服务端校验。
     *
     * @return 验证码 ID 与图片
     */
    @GetMapping("/captcha")
    public R<CaptchaResponse> captcha() {
        return R.ok(captchaService.generate());
    }
}
