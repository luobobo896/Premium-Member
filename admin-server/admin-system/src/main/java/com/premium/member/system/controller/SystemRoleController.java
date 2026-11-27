package com.premium.member.system.controller;

import java.util.List;

import com.premium.member.common.api.PageResult;
import com.premium.member.common.api.R;
import com.premium.member.framework.audit.AuditLog;
import com.premium.member.system.dto.MenuIdsRequest;
import com.premium.member.system.dto.PermissionIdsRequest;
import com.premium.member.system.dto.PermissionResponse;
import com.premium.member.system.dto.RoleCreateRequest;
import com.premium.member.system.dto.RoleQuery;
import com.premium.member.system.dto.RoleResponse;
import com.premium.member.system.dto.RoleUpdateRequest;
import com.premium.member.system.service.RoleService;
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
 * 管理端角色 CRUD 与授权。
 */
@RestController
@RequestMapping("/system/roles")
public class SystemRoleController {

    private final RoleService roleService;

    public SystemRoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 分页查询角色。
     *
     * @param query page/pageSize/code/name/status
     * @return 分页结果
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    public R<PageResult<RoleResponse>> list(@Valid RoleQuery query) {
        return R.ok(roleService.page(query));
    }

    /**
     * 全部启用角色，供用户分配角色下拉。
     *
     * @return 角色列表
     */
    @GetMapping("/options")
    @PreAuthorize("hasAuthority('system:role:list')")
    public R<List<RoleResponse>> options() {
        return R.ok(roleService.options());
    }

    /**
     * 全部权限点，供角色授权。
     *
     * @return 权限点列表
     */
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('system:role:list')")
    public R<List<PermissionResponse>> permissions() {
        return R.ok(roleService.permissions());
    }

    /**
     * 创建角色。
     *
     * @param request 创建参数
     * @return 新角色
     */
    @PostMapping
    @AuditLog(module = "角色", action = "新增")
    @PreAuthorize("hasAuthority('system:role:create')")
    public R<RoleResponse> create(@Valid @RequestBody RoleCreateRequest request) {
        return R.ok(roleService.create(request));
    }

    /**
     * 编辑角色。
     *
     * @param id      角色 ID
     * @param request 编辑参数
     * @return 更新后的角色
     */
    @PutMapping("/{id}")
    @AuditLog(module = "角色", action = "编辑")
    @PreAuthorize("hasAuthority('system:role:update')")
    public R<RoleResponse> update(@PathVariable long id, @Valid @RequestBody RoleUpdateRequest request) {
        return R.ok(roleService.update(id, request));
    }

    /**
     * 删除角色。
     *
     * @param id 角色 ID
     * @return 空数据响应
     */
    @DeleteMapping("/{id}")
    @AuditLog(module = "角色", action = "删除")
    @PreAuthorize("hasAuthority('system:role:delete')")
    public R<Void> delete(@PathVariable long id) {
        roleService.delete(id);
        return R.ok();
    }

    /**
     * 查询角色已分配菜单。
     *
     * @param id 角色 ID
     * @return 菜单 ID 列表
     */
    @GetMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:list')")
    public R<List<Long>> menuIds(@PathVariable long id) {
        return R.ok(roleService.menuIds(id));
    }

    /**
     * 分配菜单。
     *
     * @param id      角色 ID
     * @param request 菜单 ID 列表
     * @return 空数据响应
     */
    @PutMapping("/{id}/menus")
    @AuditLog(module = "角色", action = "分配菜单")
    @PreAuthorize("hasAuthority('system:role:update')")
    public R<Void> assignMenus(@PathVariable long id, @Valid @RequestBody MenuIdsRequest request) {
        roleService.assignMenus(id, request.menuIds());
        return R.ok();
    }

    /**
     * 查询角色已分配权限点。
     *
     * @param id 角色 ID
     * @return 权限 ID 列表
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role:list')")
    public R<List<Long>> permissionIds(@PathVariable long id) {
        return R.ok(roleService.permissionIds(id));
    }

    /**
     * 分配权限点。
     *
     * @param id      角色 ID
     * @param request 权限 ID 列表
     * @return 空数据响应
     */
    @PutMapping("/{id}/permissions")
    @AuditLog(module = "角色", action = "分配权限")
    @PreAuthorize("hasAuthority('system:role:update')")
    public R<Void> assignPermissions(@PathVariable long id, @Valid @RequestBody PermissionIdsRequest request) {
        roleService.assignPermissions(id, request.permissionIds());
        return R.ok();
    }
}
