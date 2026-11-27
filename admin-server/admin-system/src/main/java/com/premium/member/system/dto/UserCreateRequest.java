package com.premium.member.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建用户请求。
 *
 * @param username    登录名，3-32 位字母、数字或下划线
 * @param password    初始密码，6-64 位
 * @param realName    昵称
 * @param email       邮箱，可空
 * @param phone       手机号，可空
 * @param deptId      部门 ID，可空
 * @param status      状态：1 启用，0 停用
 * @param description 描述
 */
public record UserCreateRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_]{3,32}$", message = "登录名只能包含字母、数字和下划线，长度 3-32") String username,
        @NotBlank @Size(min = 6, max = 64) String password,
        @NotBlank @Size(max = 64) String realName,
        @Email @Size(max = 128) String email,
        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
        Long deptId,
        @Min(0) @Max(1) Integer status,
        @Size(max = 255) String description) {
}
