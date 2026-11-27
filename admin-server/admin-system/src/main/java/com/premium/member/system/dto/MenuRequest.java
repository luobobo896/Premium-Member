package com.premium.member.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 菜单新增/编辑请求。
 *
 * @param parentId       父菜单 ID，顶级为 0
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
 * @param visible        是否可见：1 可见，0 隐藏
 * @param keepAlive      是否 KeepAlive
 * @param affixTab       是否固定标签页
 * @param hideInMenu     是否在菜单隐藏
 * @param link           外链地址
 * @param iframeSrc      iframe 地址
 * @param badge          徽标文字
 * @param badgeType      徽标类型
 * @param badgeVariants  徽标颜色
 */
public record MenuRequest(
        Long parentId,
        @NotBlank @Size(max = 64) String name,
        @Size(max = 255) String path,
        @Size(max = 255) String component,
        @Size(max = 255) String redirect,
        @NotBlank @Pattern(regexp = "catalog|menu|button|embedded|link", message = "菜单类型不合法") String type,
        @NotBlank @Size(max = 128) String title,
        @Size(max = 128) String icon,
        @Size(max = 128) String permissionCode,
        @Min(0) Integer sort,
        @Min(0) @Max(1) Integer status,
        @Min(0) @Max(1) Integer visible,
        @Min(0) @Max(1) Integer keepAlive,
        @Min(0) @Max(1) Integer affixTab,
        @Min(0) @Max(1) Integer hideInMenu,
        @Size(max = 512) String link,
        @Size(max = 512) String iframeSrc,
        @Size(max = 32) String badge,
        @Size(max = 16) String badgeType,
        @Size(max = 32) String badgeVariants) {
}
