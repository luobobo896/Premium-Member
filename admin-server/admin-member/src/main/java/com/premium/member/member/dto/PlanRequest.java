package com.premium.member.member.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新增套餐或生成新版本。
 */
public record PlanRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 128) String name,
        @NotNull @Min(0) Long priceCents,
        @Min(1) Integer durationDays,
        @Min(0) Integer aiImageQuota,
        @Min(0) Integer aiVideoQuota,
        @Min(0) Integer monthlyPoints,
        @Min(0) Integer benefitQuota,
        Integer status) {
}
