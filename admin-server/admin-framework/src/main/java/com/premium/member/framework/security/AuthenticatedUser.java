package com.premium.member.framework.security;

import java.util.List;

/**
 * 当前请求的认证主体。
 *
 * @param userId   用户 ID
 * @param username 用户名
 * @param realName 昵称
 * @param roles    角色码
 */
public record AuthenticatedUser(long userId, String username, String realName, List<String> roles) {
}
