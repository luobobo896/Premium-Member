package com.premium.member.system.dto;

import java.time.OffsetDateTime;

import com.premium.member.system.entity.SysRole;

/**
 * 角色响应。
 *
 * @param id        角色 ID
 * @param code      角色码
 * @param name      角色名
 * @param status    状态：1 启用，0 停用
 * @param sort      排序
 * @param remark    备注
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record RoleResponse(
        Long id,
        String code,
        String name,
        Integer status,
        Integer sort,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static RoleResponse from(SysRole role) {
        return new RoleResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getStatus(),
                role.getSort(),
                role.getRemark(),
                role.getCreatedAt(),
                role.getUpdatedAt());
    }
}
