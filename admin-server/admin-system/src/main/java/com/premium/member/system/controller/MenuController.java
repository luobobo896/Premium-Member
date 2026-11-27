package com.premium.member.system.controller;

import java.util.List;

import com.premium.member.common.api.R;
import com.premium.member.framework.security.SecurityUtils;
import com.premium.member.system.dto.MenuRoute;
import com.premium.member.system.service.MenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * 返回当前用户的路由树，供 vben 后端权限模式使用。
     *
     * @return 路由树
     */
    @GetMapping("/all")
    public R<List<MenuRoute>> all() {
        return R.ok(menuService.routesForUser(SecurityUtils.currentUser().userId()));
    }
}
