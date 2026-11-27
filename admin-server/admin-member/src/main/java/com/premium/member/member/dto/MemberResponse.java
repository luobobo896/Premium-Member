package com.premium.member.member.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.premium.member.member.entity.PmMember;

/**
 * 会员响应。
 *
 * @param id        会员 ID
 * @param memberNo  会员编号
 * @param username  登录名
 * @param nickname  昵称
 * @param phone     手机号
 * @param level     等级
 * @param points    积分
 * @param balance   余额，单位元
 * @param status    状态：1 启用，0 停用
 * @param joinedAt  入会时间
 * @param remark    备注
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record MemberResponse(
        Long id,
        String memberNo,
        String username,
        String nickname,
        String phone,
        String level,
        Integer points,
        BigDecimal balance,
        Integer status,
        OffsetDateTime joinedAt,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static MemberResponse from(PmMember member) {
        return new MemberResponse(
                member.getId(),
                member.getMemberNo(),
                member.getUsername(),
                member.getNickname(),
                member.getPhone(),
                member.getLevel(),
                member.getPoints(),
                member.getBalance(),
                member.getStatus(),
                member.getJoinedAt(),
                member.getRemark(),
                member.getCreatedAt(),
                member.getUpdatedAt());
    }
}
