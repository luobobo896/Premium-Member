package com.premium.member.system.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.premium.member.system.dto.MenuMeta;
import com.premium.member.system.dto.MenuNode;
import com.premium.member.system.dto.MenuRoute;
import com.premium.member.system.entity.SysMenu;
import com.premium.member.system.mapper.SysMenuMapper;
import org.springframework.stereotype.Service;

@Service
public class MenuService {

    private final SysMenuMapper menuMapper;

    public MenuService(SysMenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    /**
     * 生成 vben 后端模式路由树，按钮节点不进入路由。
     *
     * @param userId 用户 ID
     * @return 路由树
     */
    public List<MenuRoute> routesForUser(long userId) {
        List<SysMenu> menus = menuMapper.selectMenusByUserId(userId).stream()
                .filter(menu -> !"button".equals(menu.getType()))
                .toList();
        return buildRoutes(roots(menus), childrenByParent(menus), SysMenu::getId);
    }

    /**
     * 管理端完整菜单树，包含按钮权限标识。
     *
     * @return 菜单树
     */
    public List<MenuNode> allMenus() {
        List<SysMenu> menus = menuMapper.selectAllActive();
        return buildNodes(roots(menus), childrenByParent(menus));
    }

    private List<MenuRoute> buildRoutes(List<SysMenu> roots, Map<Long, List<SysMenu>> childrenByParent,
            Function<SysMenu, Long> id) {
        return roots.stream()
                .map(menu -> new MenuRoute(
                        menu.getId(),
                        menu.getName(),
                        menu.getPath(),
                        routeComponent(menu),
                        menu.getRedirect(),
                        meta(menu),
                        buildRoutes(childrenByParent.getOrDefault(id.apply(menu), List.of()),
                                childrenByParent, id)))
                .toList();
    }

    private List<MenuNode> buildNodes(List<SysMenu> roots, Map<Long, List<SysMenu>> childrenByParent) {
        return roots.stream()
                .map(menu -> new MenuNode(
                        menu.getId(),
                        menu.getParentId(),
                        menu.getName(),
                        menu.getPath(),
                        menu.getComponent(),
                        menu.getRedirect(),
                        menu.getType(),
                        menu.getTitle(),
                        menu.getIcon(),
                        menu.getPermissionCode(),
                        menu.getSort(),
                        menu.getStatus(),
                        menu.getVisible(),
                        buildNodes(childrenByParent.getOrDefault(menu.getId(), List.of()), childrenByParent)))
                .toList();
    }

    private List<SysMenu> roots(List<SysMenu> menus) {
        Set<Long> ids = menus.stream().map(SysMenu::getId).collect(Collectors.toSet());
        return menus.stream()
                .filter(menu -> menu.getParentId() == null || menu.getParentId() == 0 || !ids.contains(menu.getParentId()))
                .toList();
    }

    private Map<Long, List<SysMenu>> childrenByParent(List<SysMenu> menus) {
        return menus.stream().collect(Collectors.groupingBy(
                menu -> menu.getParentId() == null ? 0L : menu.getParentId(),
                Collectors.mapping(Function.identity(), Collectors.toCollection(ArrayList::new))));
    }

    private String routeComponent(SysMenu menu) {
        if (menu.getComponent() != null && !menu.getComponent().isBlank()) {
            return menu.getComponent();
        }
        if ("link".equals(menu.getType()) || "embedded".equals(menu.getType())) {
            return "IFrameView";
        }
        return null;
    }

    private MenuMeta meta(SysMenu menu) {
        return new MenuMeta(
                menu.getTitle(),
                menu.getIcon(),
                menu.getSort(),
                Integer.valueOf(1).equals(menu.getKeepAlive()),
                Integer.valueOf(1).equals(menu.getAffixTab()),
                Integer.valueOf(1).equals(menu.getHideInMenu()) || Integer.valueOf(0).equals(menu.getVisible()),
                menu.getLink(),
                menu.getIframeSrc(),
                menu.getBadge(),
                menu.getBadgeType(),
                menu.getBadgeVariants());
    }
}
