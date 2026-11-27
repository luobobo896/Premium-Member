package com.premium.member.system.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.premium.member.common.exception.BusinessException;
import com.premium.member.common.exception.ErrorCode;
import com.premium.member.framework.cache.PermissionCache;
import com.premium.member.framework.cache.TokenStore;
import com.premium.member.framework.security.AccessClaims;
import com.premium.member.framework.security.AuthUser;
import com.premium.member.framework.security.JwtProperties;
import com.premium.member.framework.security.JwtTokenService;
import com.premium.member.framework.security.RefreshClaims;
import com.premium.member.framework.security.TokenValue;
import com.premium.member.system.dto.LoginRequest;
import com.premium.member.system.dto.LoginResponse;
import com.premium.member.system.dto.LoginResult;
import com.premium.member.system.dto.UserInfoResponse;
import com.premium.member.system.entity.SysUser;
import com.premium.member.system.mapper.SysPermissionMapper;
import com.premium.member.system.mapper.SysRoleMapper;
import com.premium.member.system.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final JwtProperties jwtProperties;
    private final TokenStore tokenStore;
    private final PermissionCache permissionCache;
    private final CaptchaService captchaService;

    public AuthService(SysUserMapper userMapper, SysRoleMapper roleMapper, SysPermissionMapper permissionMapper,
            PasswordEncoder passwordEncoder, JwtTokenService tokenService, JwtProperties jwtProperties,
            TokenStore tokenStore, PermissionCache permissionCache, CaptchaService captchaService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.jwtProperties = jwtProperties;
        this.tokenStore = tokenStore;
        this.permissionCache = permissionCache;
        this.captchaService = captchaService;
    }

    /**
     * 用户名密码登录。
     *
     * @param request 登录参数
     * @return 响应体与 refresh token
     */
    public LoginResult login(LoginRequest request) {
        if (StringUtils.hasText(request.captchaId())) {
            captchaService.validate(request.captchaId(), request.captchaCode());
        }
        SysUser user = userMapper.selectByUsername(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        ensureActive(user);

        AuthUser authUser = toAuthUser(user);
        TokenValue accessToken = tokenService.createAccessToken(authUser);
        TokenValue refreshToken = tokenService.createRefreshToken(authUser);
        try {
            tokenStore.saveRefreshToken(refreshToken.jti(), user.getId(), jwtProperties.getRefreshTtl());
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable, refresh token was not persisted", ex);
        }
        LoginResponse body = new LoginResponse(
                accessToken.value(),
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                valueOrEmpty(user.getAvatar()),
                valueOrEmpty(user.getDescription()),
                valueOrDefault(user.getHomePath(), "/analytics"),
                authUser.roles());
        return new LoginResult(body, refreshToken.value());
    }

    /**
     * 用 refresh token 换取新的 access token。
     *
     * @param refreshToken Cookie 中的 refresh token
     * @return 新 access token
     */
    public String refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        RefreshClaims claims;
        try {
            claims = tokenService.parseRefreshToken(refreshToken);
        } catch (BusinessException ex) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        boolean active;
        try {
            active = tokenStore.isRefreshTokenActive(claims.jti(), claims.userId());
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable while checking refresh token", ex);
            active = false;
        }
        if (!active) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        SysUser user = userMapper.selectActiveById(claims.userId());
        if (user == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        ensureActive(user);
        return tokenService.createAccessToken(toAuthUser(user)).value();
    }

    /**
     * 注销：撤销 refresh token 白名单，并尽可能把当前 access token 加入黑名单。
     *
     * @param refreshToken       Cookie 中的 refresh token
     * @param authorizationHeader Authorization 请求头
     */
    public void logout(String refreshToken, String authorizationHeader) {
        if (StringUtils.hasText(refreshToken)) {
            try {
                RefreshClaims claims = tokenService.parseRefreshToken(refreshToken);
                tokenStore.revokeRefreshToken(claims.jti());
            } catch (RuntimeException ex) {
                log.debug("Ignore invalid refresh token on logout", ex);
            }
        }
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                AccessClaims claims = tokenService.parseAccessToken(authorizationHeader.substring(7));
                tokenStore.revokeAccessToken(claims.jti(),
                        Duration.between(Instant.now(), claims.expiresAt()));
            } catch (RuntimeException ex) {
                log.debug("Ignore invalid access token on logout", ex);
            }
        }
    }

    /**
     * 获取用户权限码。
     *
     * @param userId 用户 ID
     * @return 权限码
     */
    public List<String> accessCodes(long userId) {
        return loadPermissions(userId);
    }

    /**
     * 获取用户信息。
     *
     * @param userId 用户 ID
     * @return vben 用户信息
     */
    public UserInfoResponse userInfo(long userId) {
        SysUser user = userMapper.selectActiveById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        ensureActive(user);
        return new UserInfoResponse(
                String.valueOf(user.getId()),
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                valueOrEmpty(user.getAvatar()),
                valueOrEmpty(user.getDescription()),
                valueOrDefault(user.getHomePath(), "/analytics"),
                roleMapper.selectCodesByUserId(userId));
    }

    private AuthUser toAuthUser(SysUser user) {
        return new AuthUser(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                valueOrEmpty(user.getAvatar()),
                valueOrEmpty(user.getDescription()),
                valueOrDefault(user.getHomePath(), "/analytics"),
                roleMapper.selectCodesByUserId(user.getId()),
                loadPermissions(user.getId()));
    }

    private List<String> loadPermissions(long userId) {
        try {
            Optional<List<String>> cached = permissionCache.get(userId);
            if (cached.isPresent()) {
                return cached.get();
            }
            List<String> permissions = permissionMapper.selectCodesByUserId(userId);
            permissionCache.put(userId, permissions);
            return permissions;
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable while loading permissions, fallback to database", ex);
            return permissionMapper.selectCodesByUserId(userId);
        }
    }

    private void ensureActive(SysUser user) {
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
