package com.premium.member.c.biz.member.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.premium.member.c.biz.member.entity.PmMember;

/**
 * C 端当前会员信息。
 */
public record MemberMeResponse(
        Long id,
        String memberNo,
        String username,
        String nickname,
        String phone,
        String level,
        Integer points,
        BigDecimal balance,
        Integer status,
        OffsetDateTime joinedAt) {

    public static MemberMeResponse from(PmMember member) {
        return new MemberMeResponse(
                member.getId(),
                member.getMemberNo(),
                member.getUsername(),
                member.getNickname(),
                member.getPhone(),
                member.getLevel(),
                member.getPoints(),
                member.getBalance(),
                member.getStatus(),
                member.getJoinedAt());
    }
}
