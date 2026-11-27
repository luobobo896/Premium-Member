package com.premium.member.c.user.dto;

import com.premium.member.c.user.entity.CUser;

/**
 * C 端用户信息。
 *
 * @param id       用户 ID
 * @param phone    手机号
 * @param nickname 昵称
 * @param avatar   头像
 */
public record CUserInfoResponse(Long id, String phone, String nickname, String avatar) {

    public static CUserInfoResponse from(CUser user) {
        return new CUserInfoResponse(user.getId(), user.getPhone(), user.getNickname(), user.getAvatar());
    }
}
