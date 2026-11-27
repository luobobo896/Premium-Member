package com.premium.member.system.controller;

import java.util.List;

import com.premium.member.common.api.R;
import com.premium.member.system.dto.DeptNode;
import com.premium.member.system.service.DeptService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/dept")
public class DeptController {

    private final DeptService deptService;

    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    /**
     * 返回部门树。
     *
     * @return 部门树
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:dept:list')")
    public R<List<DeptNode>> list() {
        return R.ok(deptService.tree());
    }
}
