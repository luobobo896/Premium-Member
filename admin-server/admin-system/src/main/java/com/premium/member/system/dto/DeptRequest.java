package com.premium.member.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 部门新增/编辑请求。
 *
 * @param parentId 父部门 ID，顶级为 0
 * @param name     部门名称
 * @param leader   负责人
 * @param phone    联系电话
 * @param sort     排序
 * @param status   状态：1 启用，0 停用
 */
public record DeptRequest(
        Long parentId,
        @NotBlank @Size(max = 64) String name,
        @Size(max = 64) String leader,
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
        @Min(0) Integer sort,
        @Min(0) @Max(1) Integer status) {
}
