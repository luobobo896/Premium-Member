package com.premium.member.member.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 权益商品新增/编辑参数。
 *
 * @param skuCode     商品编码，创建后不可修改
 * @param name        名称
 * @param description 描述
 * @param rules       兑换说明
 * @param status      1 上架，0 下架
 * @param sort        排序
 */
public record BenefitProductRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "仅允许字母、数字、下划线和中划线")
        String skuCode,
        @NotBlank @Size(max = 128) String name,
        @Size(max = 2000) String description,
        @Size(max = 4000) String rules,
        @Min(0) @Max(1) Integer status,
        Integer sort) {
}
