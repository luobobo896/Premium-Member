package com.premium.member.c.user.service;

import com.premium.member.c.common.exception.BusinessException;
import com.premium.member.c.common.exception.ErrorCode;
import com.premium.member.c.framework.security.JwtProperties;
import com.premium.member.c.framework.security.JwtTokenService;
import com.premium.member.c.framework.security.RefreshTokenStore;
import com.premium.member.c.user.dto.CUserInfoResponse;
import com.premium.member.c.user.dto.LoginRequest;
import com.premium.member.c.user.dto.RegisterRequest;
import com.premium.member.c.user.entity.CUser;
import com.premium.member.c.user.mapper.CUserMapper;
import io.jsonwebtoken.Claims;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * C 端账号注册、登录与 token 管理。
 */
@Service
public class CUserService {

    private final CUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenStore refreshTokenStore;

    public CUserService(CUserMapper userMapper, PasswordEncoder passwordEncoder,
            JwtTokenService tokenService, JwtProperties jwtProperties,
            RefreshTokenStore refreshTokenStore) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.jwtProperties = jwtProperties;
        this.refreshTokenStore = refreshTokenStore;
    }

    @Transactional
    public CUserInfoResponse register(RegisterRequest request) {
        String phone = request.phone().trim();
        if (userMapper.existsByPhone(phone)) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
        }
        CUser user = new CUser();
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(StringUtils.hasText(request.nickname()) ? request.nickname().trim() : phone);
        user.setAvatar("");
        user.setStatus(1);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
        }
        return CUserInfoResponse.from(user);
    }

    public LoginSession login(LoginRequest request) {
        CUser user = userMapper.selectByPhone(request.phone().trim());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        ensureActive(user);
        String accessToken = tokenService.createAccessToken(user.getId(), user.getPhone());
        String refreshToken = tokenService.createRefreshToken(user.getId(), user.getPhone());
        refreshTokenStore.save(refreshJti(refreshToken), user.getId(), jwtProperties.getRefreshTtl());
        return new LoginSession(accessToken, refreshToken, CUserInfoResponse.from(user));
    }

    public String refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        Claims claims;
        try {
            claims = tokenService.parseRefreshToken(refreshToken);
        } catch (BusinessException ex) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        long userId = ((Number) claims.get("uid")).longValue();
        if (!refreshTokenStore.isActive(claims.getId(), userId)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        CUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        ensureActive(user);
        return tokenService.createAccessToken(user.getId(), user.getPhone());
    }

    public void logout(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        try {
            refreshTokenStore.revoke(refreshJti(refreshToken));
        } catch (RuntimeException ignored) {
            // 无效 refresh token 直接忽略，Cookie 仍会被清除。
        }
    }

    public CUserInfoResponse info(long userId) {
        CUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        ensureActive(user);
        return CUserInfoResponse.from(user);
    }

    private String refreshJti(String refreshToken) {
        return tokenService.parseRefreshToken(refreshToken).getId();
    }

    private void ensureActive(CUser user) {
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
    }

    /**
     * 登录会话：refresh token 只写入 Cookie，不进入响应体。
     *
     * @param accessToken  access token
     * @param refreshToken refresh token
     * @param user         用户信息
     */
    public record LoginSession(String accessToken, String refreshToken, CUserInfoResponse user) {
    }
}
