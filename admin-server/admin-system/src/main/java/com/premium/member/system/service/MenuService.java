package com.premium.member.system.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.premium.member.common.exception.BusinessException;
import com.premium.member.common.exception.ErrorCode;
import com.premium.member.system.dto.MenuRequest;
import com.premium.member.system.dto.MenuMeta;
import com.premium.member.system.dto.MenuNode;
import com.premium.member.system.dto.MenuRoute;
import com.premium.member.system.dto.MenuTreeNode;
import com.premium.member.system.entity.SysMenu;
import com.premium.member.system.entity.SysPermission;
import com.premium.member.system.mapper.SysMenuMapper;
import com.premium.member.system.mapper.SysPermissionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MenuService {

    private final SysMenuMapper menuMapper;
    private final SysPermissionMapper permissionMapper;

    public MenuService(SysMenuMapper menuMapper, SysPermissionMapper permissionMapper) {
        this.menuMapper = menuMapper;
        this.permissionMapper = permissionMapper;
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

    /**
     * 管理端菜单树，包含停用节点，并支持标题与状态过滤（保留匹配节点的祖先）。
     *
     * @param title  标题模糊匹配，可空
     * @param status 状态过滤，可空
     * @return 菜单树
     */
    public List<MenuTreeNode> manageTree(String title, Integer status) {
        List<SysMenu> all = menuMapper.selectAll();
        List<SysMenu> filtered = filterWithAncestors(all, title, status);
        Map<Long, List<SysMenu>> children = childrenByParent(filtered);
        return buildManageNodes(roots(filtered), children);
    }

    /**
     * 创建菜单，权限标识会自动补一条权限点，便于角色授权。
     *
     * @param request 创建参数
     * @return 新菜单
     */
    @Transactional
    public MenuTreeNode create(MenuRequest request) {
        validateParent(request.parentId(), null);
        OffsetDateTime now = OffsetDateTime.now();
        SysMenu menu = new SysMenu();
        apply(menu, request);
        menu.setCreatedAt(now);
        menu.setUpdatedAt(now);
        menu.setDeleted(0);
        if (menuMapper.insert(menu) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "菜单创建失败");
        }
        ensurePermission(menu);
        return MenuTreeNode.from(menu, List.of());
    }

    /**
     * 编辑菜单。
     *
     * @param id      菜单 ID
     * @param request 编辑参数
     * @return 更新后的菜单
     */
    @Transactional
    public MenuTreeNode update(long id, MenuRequest request) {
        SysMenu menu = requireMenu(id);
        validateParent(request.parentId(), id);
        apply(menu, request);
        menu.setUpdatedAt(OffsetDateTime.now());
        if (menuMapper.updateById(menu) != 1) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }
        ensurePermission(menu);
        return MenuTreeNode.from(menu, List.of());
    }

    /**
     * 删除菜单，存在子节点或角色引用时拒绝。
     *
     * @param id 菜单 ID
     */
    @Transactional
    public void delete(long id) {
        requireMenu(id);
        if (menuMapper.countChildren(id) > 0) {
            throw new BusinessException(ErrorCode.MENU_HAS_CHILDREN);
        }
        if (menuMapper.countRoleRefs(id) > 0) {
            throw new BusinessException(ErrorCode.MENU_IN_USE);
        }
        if (menuMapper.deleteById(id) != 1) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }
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

    private List<MenuTreeNode> buildManageNodes(List<SysMenu> roots, Map<Long, List<SysMenu>> childrenByParent) {
        return roots.stream()
                .map(menu -> MenuTreeNode.from(menu, buildManageNodes(
                        childrenByParent.getOrDefault(menu.getId(), List.of()), childrenByParent)))
                .toList();
    }

    private List<SysMenu> filterWithAncestors(List<SysMenu> menus, String title, Integer status) {
        if (!StringUtils.hasText(title) && status == null) {
            return menus;
        }
        Map<Long, SysMenu> byId = menus.stream().collect(Collectors.toMap(SysMenu::getId, Function.identity()));
        Set<Long> keep = new HashSet<>();
        for (SysMenu menu : menus) {
            boolean matches = (!StringUtils.hasText(title)
                    || menu.getTitle() != null && menu.getTitle().contains(title.trim()))
                    && (status == null || status.equals(menu.getStatus()));
            if (!matches) {
                continue;
            }
            SysMenu cursor = menu;
            int guard = 0;
            while (cursor != null && guard++ < 100) {
                keep.add(cursor.getId());
                cursor = cursor.getParentId() == null || cursor.getParentId() == 0
                        ? null
                        : byId.get(cursor.getParentId());
            }
        }
        return menus.stream().filter(menu -> keep.contains(menu.getId())).toList();
    }

    private void apply(SysMenu menu, MenuRequest request) {
        menu.setParentId(request.parentId() == null ? 0L : request.parentId());
        menu.setName(request.name().trim());
        menu.setPath(valueOrEmpty(request.path()));
        menu.setComponent(valueOrNull(request.component()));
        menu.setRedirect(valueOrNull(request.redirect()));
        menu.setType(request.type().trim());
        menu.setTitle(request.title().trim());
        menu.setIcon(valueOrNull(request.icon()));
        menu.setPermissionCode(valueOrNull(request.permissionCode()));
        menu.setSort(request.sort() == null ? 0 : request.sort());
        menu.setStatus(request.status() == null ? 1 : request.status());
        menu.setVisible(request.visible() == null ? 1 : request.visible());
        menu.setKeepAlive(request.keepAlive() == null ? 0 : request.keepAlive());
        menu.setAffixTab(request.affixTab() == null ? 0 : request.affixTab());
        menu.setHideInMenu(request.hideInMenu() == null ? 0 : request.hideInMenu());
        menu.setLink(valueOrNull(request.link()));
        menu.setIframeSrc(valueOrNull(request.iframeSrc()));
        menu.setBadge(valueOrNull(request.badge()));
        menu.setBadgeType(valueOrNull(request.badgeType()));
        menu.setBadgeVariants(valueOrNull(request.badgeVariants()));
    }

    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (parentId.equals(selfId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "父级不能是自身");
        }
        SysMenu cursor = menuMapper.selectById(parentId);
        if (cursor == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND, "父级菜单不存在");
        }
        int guard = 0;
        while (cursor != null && cursor.getParentId() != null && cursor.getParentId() != 0 && guard++ < 100) {
            if (Objects.equals(cursor.getId(), selfId)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "父级不能是自身的子级");
            }
            cursor = menuMapper.selectById(cursor.getParentId());
        }
    }

    private void ensurePermission(SysMenu menu) {
        String code = menu.getPermissionCode();
        if (!StringUtils.hasText(code) || permissionMapper.selectByCode(code) != null) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        SysPermission permission = new SysPermission();
        permission.setCode(code);
        permission.setName(menu.getTitle());
        permission.setType("button".equals(menu.getType()) ? "BUTTON" : "API");
        permission.setDescription("菜单自动创建：" + menu.getTitle());
        permission.setCreatedAt(now);
        permission.setUpdatedAt(now);
        permission.setDeleted(0);
        permissionMapper.insert(permission);
    }

    private SysMenu requireMenu(long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(ErrorCode.MENU_NOT_FOUND);
        }
        return menu;
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

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String valueOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
