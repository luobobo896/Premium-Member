package com.premium.member.system.dto;

import com.premium.member.system.entity.SysPermission;

/**
 * 权限点响应。
 *
 * @param id          权限 ID
 * @param code        权限码
 * @param name        权限名称
 * @param type        类型：API 或 BUTTON
 * @param description 说明
 */
public record PermissionResponse(Long id, String code, String name, String type, String description) {

    public static PermissionResponse from(SysPermission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getType(),
                permission.getDescription());
    }
}
