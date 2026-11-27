package com.premium.member.system.controller;

import java.util.List;

import com.premium.member.common.api.R;
import com.premium.member.system.dto.MenuRequest;
import com.premium.member.system.dto.MenuTreeNode;
import com.premium.member.system.service.MenuService;
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
 * 管理端菜单 CRUD。
 */
@RestController
@RequestMapping("/system/menus")
public class SystemMenuManageController {

    private final MenuService menuService;

    public SystemMenuManageController(MenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * 查询完整菜单树。
     *
     * @param title  标题模糊匹配，可空
     * @param status 状态过滤，可空
     * @return 菜单树
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:menu:list')")
    public R<List<MenuTreeNode>> list(@RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status) {
        return R.ok(menuService.manageTree(title, status));
    }

    /**
     * 创建菜单。
     *
     * @param request 创建参数
     * @return 新菜单
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:create')")
    public R<MenuTreeNode> create(@Valid @RequestBody MenuRequest request) {
        return R.ok(menuService.create(request));
    }

    /**
     * 编辑菜单。
     *
     * @param id      菜单 ID
     * @param request 编辑参数
     * @return 更新后的菜单
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:update')")
    public R<MenuTreeNode> update(@PathVariable long id, @Valid @RequestBody MenuRequest request) {
        return R.ok(menuService.update(id, request));
    }

    /**
     * 删除菜单。
     *
     * @param id 菜单 ID
     * @return 空数据响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:delete')")
    public R<Void> delete(@PathVariable long id) {
        menuService.delete(id);
        return R.ok();
    }
}
