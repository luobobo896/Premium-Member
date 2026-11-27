package com.premium.member.framework.security;

import java.util.List;

/**
 * 登录用户快照，用于签发 token 和构造 SecurityContext。
 *
 * @param userId      用户 ID
 * @param username    用户名
 * @param realName    昵称
 * @param avatar      头像地址
 * @param description 用户描述
 * @param homePath    登录后首页
 * @param roles       角色码
 * @param permissions 权限码
 */
public record AuthUser(
        long userId,
        String username,
        String realName,
        String avatar,
        String description,
        String homePath,
        List<String> roles,
        List<String> permissions) {
}
