package com.premium.member.system.controller;

import java.util.List;

import com.premium.member.common.api.R;
import com.premium.member.system.dto.MenuNode;
import com.premium.member.system.service.MenuService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/menu")
public class SystemMenuController {

    private final MenuService menuService;

    public SystemMenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * 管理端菜单树，包含按钮权限标识。
     *
     * @return 菜单树
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public R<List<MenuNode>> list() {
        return R.ok(menuService.allMenus());
    }
}
