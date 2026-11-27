package com.premium.member.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 启用/停用请求。
 *
 * @param status 状态：1 启用，0 停用
 */
public record StatusRequest(@NotNull @Min(0) @Max(1) Integer status) {
}
