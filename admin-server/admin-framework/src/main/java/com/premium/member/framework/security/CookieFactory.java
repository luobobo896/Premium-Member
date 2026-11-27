package com.premium.member.framework.security;

import java.util.Arrays;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * refresh token Cookie 的读写。
 */
@Component
public class CookieFactory {

    private final CookieProperties properties;

    public CookieFactory(CookieProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie create(String value) {
        return base(value).maxAge(properties.getMaxAge()).build();
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
                .filter(cookie -> properties.getName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(properties.getName(), value)
                .httpOnly(true)
                .secure(properties.isSecure())
                .sameSite(properties.isSecure() ? "None" : "Lax")
                .path(properties.getPath());
    }
}
