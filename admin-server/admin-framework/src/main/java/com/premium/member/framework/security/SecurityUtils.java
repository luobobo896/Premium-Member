package com.premium.member.framework.security;

import com.premium.member.common.exception.BusinessException;
import com.premium.member.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 读取当前请求的认证主体。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户，未认证时抛 401。
     *
     * @return 当前用户
     */
    public static AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
