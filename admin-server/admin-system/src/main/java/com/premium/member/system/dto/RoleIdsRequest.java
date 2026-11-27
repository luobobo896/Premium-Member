package com.premium.member.system.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * 分配角色请求。
 *
 * @param roleIds 角色 ID 列表，空列表表示清空
 */
public record RoleIdsRequest(@NotNull List<Long> roleIds) {
}
