package com.premium.member.system.dto;

import java.util.List;

/**
 * 登录响应。vben 只读取 accessToken，其余字段便于调试和前端直接展示。
 *
 * @param accessToken access token
 * @param userId      用户 ID
 * @param username    登录名
 * @param realName    昵称
 * @param avatar      头像
 * @param description 用户描述
 * @param homePath    登录后首页
 * @param roles       角色码
 */
public record LoginResponse(
        String accessToken,
        long userId,
        String username,
        String realName,
        String avatar,
        String description,
        String homePath,
        List<String> roles) {
}
