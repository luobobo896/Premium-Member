package com.premium.member.system.controller;

import com.premium.member.common.api.PageResult;
import com.premium.member.common.api.R;
import com.premium.member.framework.audit.AuditLog;
import com.premium.member.system.dto.PasswordResetRequest;
import com.premium.member.system.dto.RoleIdsRequest;
import com.premium.member.system.dto.StatusRequest;
import com.premium.member.system.dto.UserCreateRequest;
import com.premium.member.system.dto.UserQuery;
import com.premium.member.system.dto.UserResponse;
import com.premium.member.system.dto.UserUpdateRequest;
import com.premium.member.system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端用户 CRUD。
 */
@RestController
@RequestMapping("/system/users")
public class SystemUserController {

    private final UserService userService;

    public SystemUserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 分页查询用户。
     *
     * @param query page/pageSize/username/realName/status/deptId
     * @return 分页结果
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<PageResult<UserResponse>> list(@Valid UserQuery query) {
        return R.ok(userService.page(query));
    }

    /**
     * 创建用户。
     *
     * @param request 创建参数
     * @return 新用户
     */
    @PostMapping
    @AuditLog(module = "用户", action = "新增")
    @PreAuthorize("hasAuthority('system:user:create')")
    public R<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return R.ok(userService.create(request));
    }

    /**
     * 编辑用户。
     *
     * @param id      用户 ID
     * @param request 编辑参数
     * @return 更新后的用户
     */
    @PutMapping("/{id}")
    @AuditLog(module = "用户", action = "编辑")
    @PreAuthorize("hasAuthority('system:user:update')")
    public R<UserResponse> update(@PathVariable long id, @Valid @RequestBody UserUpdateRequest request) {
        return R.ok(userService.update(id, request));
    }

    /**
     * 删除用户。
     *
     * @param id 用户 ID
     * @return 空数据响应
     */
    @DeleteMapping("/{id}")
    @AuditLog(module = "用户", action = "删除")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public R<Void> delete(@PathVariable long id) {
        userService.delete(id);
        return R.ok();
    }

    /**
     * 重置密码。
     *
     * @param id      用户 ID
     * @param request 新密码
     * @return 空数据响应
     */
    @PutMapping("/{id}/password")
    @AuditLog(module = "用户", action = "重置密码")
    @PreAuthorize("hasAuthority('system:user:update')")
    public R<Void> resetPassword(@PathVariable long id, @Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(id, request.password());
        return R.ok();
    }

    /**
     * 启用/停用用户。
     *
     * @param id      用户 ID
     * @param request 状态
     * @return 空数据响应
     */
    @PutMapping("/{id}/status")
    @AuditLog(module = "用户", action = "启停")
    @PreAuthorize("hasAuthority('system:user:update')")
    public R<Void> updateStatus(@PathVariable long id, @Valid @RequestBody StatusRequest request) {
        userService.updateStatus(id, request.status());
        return R.ok();
    }

    /**
     * 分配角色。
     *
     * @param id      用户 ID
     * @param request 角色 ID 列表
     * @return 空数据响应
     */
    @PutMapping("/{id}/roles")
    @AuditLog(module = "用户", action = "分配角色")
    @PreAuthorize("hasAuthority('system:user:update')")
    public R<Void> assignRoles(@PathVariable long id, @Valid @RequestBody RoleIdsRequest request) {
        userService.assignRoles(id, request.roleIds());
        return R.ok();
    }
}
