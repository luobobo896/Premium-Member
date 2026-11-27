package com.premium.member.system.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.premium.member.system.entity.SysUser;

/**
 * 用户响应。
 *
 * @param id          用户 ID
 * @param username    登录名
 * @param realName    昵称
 * @param email       邮箱
 * @param phone       手机号
 * @param deptId      部门 ID
 * @param deptName    部门名称
 * @param status      状态：1 启用，0 停用
 * @param description 描述
 * @param roleIds     已分配角色 ID
 * @param createdAt   创建时间
 * @param updatedAt   更新时间
 */
public record UserResponse(
        Long id,
        String username,
        String realName,
        String email,
        String phone,
        Long deptId,
        String deptName,
        Integer status,
        String description,
        List<Long> roleIds,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static UserResponse from(SysUser user, String deptName, List<Long> roleIds) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getEmail(),
                user.getPhone(),
                user.getDeptId(),
                deptName,
                user.getStatus(),
                user.getDescription(),
                roleIds,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
