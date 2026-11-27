package com.premium.member.system.dto;

import java.util.List;

import com.premium.member.system.entity.SysMenu;

/**
 * 管理端菜单树节点，包含编辑所需的全部字段。
 *
 * @param id             菜单 ID
 * @param parentId       父菜单 ID
 * @param name           路由 name
 * @param path           路由 path
 * @param component      组件路径
 * @param redirect       重定向
 * @param type           catalog/menu/button/embedded/link
 * @param title          菜单标题
 * @param icon           图标
 * @param permissionCode 权限标识
 * @param sort           排序
 * @param status         状态：1 启用，0 停用
 * @param visible        是否可见
 * @param keepAlive      是否 KeepAlive
 * @param affixTab       是否固定标签页
 * @param hideInMenu     是否在菜单隐藏
 * @param link           外链地址
 * @param iframeSrc      iframe 地址
 * @param badge          徽标文字
 * @param badgeType      徽标类型
 * @param badgeVariants  徽标颜色
 * @param children       子菜单
 */
public record MenuTreeNode(
        Long id,
        Long parentId,
        String name,
        String path,
        String component,
        String redirect,
        String type,
        String title,
        String icon,
        String permissionCode,
        Integer sort,
        Integer status,
        Integer visible,
        Integer keepAlive,
        Integer affixTab,
        Integer hideInMenu,
        String link,
        String iframeSrc,
        String badge,
        String badgeType,
        String badgeVariants,
        List<MenuTreeNode> children) {

    public static MenuTreeNode from(SysMenu menu, List<MenuTreeNode> children) {
        return new MenuTreeNode(
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
                menu.getKeepAlive(),
                menu.getAffixTab(),
                menu.getHideInMenu(),
                menu.getLink(),
                menu.getIframeSrc(),
                menu.getBadge(),
                menu.getBadgeType(),
                menu.getBadgeVariants(),
                children);
    }
}
