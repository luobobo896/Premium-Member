package com.premium.member.c.framework.security;

import java.time.Duration;
import java.util.Arrays;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * C 端 refresh token Cookie 的读写。
 */
@Component
public class RefreshCookieFactory {

    private final String name;
    private final String path;
    private final boolean secure;
    private final Duration maxAge;

    public RefreshCookieFactory(
            @Value("${app.cookie.name:c_refresh}") String name,
            @Value("${app.cookie.path:/c/auth}") String path,
            @Value("${app.cookie.secure:false}") boolean secure,
            @Value("${app.cookie.max-age:30d}") Duration maxAge) {
        this.name = name;
        this.path = path;
        this.secure = secure;
        this.maxAge = maxAge;
    }

    public ResponseCookie create(String value) {
        return base(value).maxAge(maxAge).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path(path);
    }
}
