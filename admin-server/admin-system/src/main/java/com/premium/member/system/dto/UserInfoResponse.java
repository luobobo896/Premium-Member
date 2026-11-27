package com.premium.member.system.dto;

import java.util.List;

/**
 * /user/info 响应，对齐 vben BasicUserInfo 并保留 mock 的数字 id。
 *
 * @param userId   用户 ID 字符串，vben 类型要求 string
 * @param id       数字用户 ID，兼容 mock
 * @param username 登录名
 * @param realName 昵称
 * @param avatar   头像
 * @param desc     用户描述
 * @param homePath 登录后首页
 * @param roles    角色码
 */
public record UserInfoResponse(
        String userId,
        long id,
        String username,
        String realName,
        String avatar,
        String desc,
        String homePath,
        List<String> roles) {
}
