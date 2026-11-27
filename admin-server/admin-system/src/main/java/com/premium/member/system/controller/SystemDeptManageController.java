package com.premium.member.system.controller;

import java.util.List;

import com.premium.member.common.api.R;
import com.premium.member.framework.audit.AuditLog;
import com.premium.member.system.dto.DeptNode;
import com.premium.member.system.dto.DeptRequest;
import com.premium.member.system.service.DeptService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端部门 CRUD。
 */
@RestController
@RequestMapping("/system/depts")
public class SystemDeptManageController {

    private final DeptService deptService;

    public SystemDeptManageController(DeptService deptService) {
        this.deptService = deptService;
    }

    /**
     * 查询完整部门树。
     *
     * @param name   名称模糊匹配，可空
     * @param status 状态过滤，可空
     * @return 部门树
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:dept:list')")
    public R<List<DeptNode>> list(@RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        return R.ok(deptService.manageTree(name, status));
    }

    /**
     * 创建部门。
     *
     * @param request 创建参数
     * @return 新部门
     */
    @PostMapping
    @AuditLog(module = "部门", action = "新增")
    @PreAuthorize("hasAuthority('system:dept:create')")
    public R<DeptNode> create(@Valid @RequestBody DeptRequest request) {
        return R.ok(deptService.create(request));
    }

    /**
     * 编辑部门。
     *
     * @param id      部门 ID
     * @param request 编辑参数
     * @return 更新后的部门
     */
    @PutMapping("/{id}")
    @AuditLog(module = "部门", action = "编辑")
    @PreAuthorize("hasAuthority('system:dept:update')")
    public R<DeptNode> update(@PathVariable long id, @Valid @RequestBody DeptRequest request) {
        return R.ok(deptService.update(id, request));
    }

    /**
     * 删除部门。
     *
     * @param id 部门 ID
     * @return 空数据响应
     */
    @DeleteMapping("/{id}")
    @AuditLog(module = "部门", action = "删除")
    @PreAuthorize("hasAuthority('system:dept:delete')")
    public R<Void> delete(@PathVariable long id) {
        deptService.delete(id);
        return R.ok();
    }
}
