package com.premium.member.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 编辑角色请求。
 *
 * @param code   角色码
 * @param name   角色名
 * @param dataScope 数据范围
 * @param status 状态：1 启用，0 停用
 * @param sort   排序
 * @param remark 备注
 */
public record RoleUpdateRequest(
        @NotBlank @Pattern(regexp = "^[a-z][a-z0-9_]{1,63}$", message = "角色码需为小写字母开头的字母、数字、下划线组合") String code,
        @NotBlank @Size(max = 64) String name,
        @Pattern(regexp = "ALL|DEPT_AND_CHILD|DEPT|SELF", message = "数据范围不合法") String dataScope,
        @Min(0) @Max(1) Integer status,
        @Min(0) Integer sort,
        @Size(max = 255) String remark) {
}
