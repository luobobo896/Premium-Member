package com.premium.member.system.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * 分配权限点请求。
 *
 * @param permissionIds 权限点 ID 列表，空列表表示清空
 */
public record PermissionIdsRequest(@NotNull List<Long> permissionIds) {
}
