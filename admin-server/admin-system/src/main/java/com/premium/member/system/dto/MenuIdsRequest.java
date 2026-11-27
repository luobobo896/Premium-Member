package com.premium.member.system.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * 分配菜单请求。
 *
 * @param menuIds 菜单 ID 列表，空列表表示清空
 */
public record MenuIdsRequest(@NotNull List<Long> menuIds) {
}
