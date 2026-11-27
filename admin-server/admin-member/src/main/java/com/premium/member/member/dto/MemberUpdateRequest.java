package com.premium.member.member.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 更新会员请求，字段语义同创建。
 *
 * @param username 登录名
 * @param nickname 昵称
 * @param phone    手机号
 * @param level    等级
 * @param points   积分
 * @param balance  余额
 * @param status   状态
 * @param joinedAt 入会时间
 * @param remark   备注
 */
public record MemberUpdateRequest(
        @NotBlank @Size(max = 64) String username,
        @Size(max = 64) String nickname,
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
        @Pattern(regexp = "BASIC|SILVER|GOLD|PLATINUM", message = "会员等级不合法") String level,
        @Min(0) Integer points,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal balance,
        @Min(0) @Max(1) Integer status,
        OffsetDateTime joinedAt,
        @Size(max = 255) String remark) {
}
