package com.premium.member.system.dto;

/**
 * vben 路由 meta 字段。
 *
 * @param title         菜单标题
 * @param icon          图标
 * @param order         排序
 * @param keepAlive     是否缓存页面
 * @param affixTab      是否固定标签页
 * @param hideInMenu    是否在菜单隐藏
 * @param link          外链地址
 * @param iframeSrc     iframe 地址
 * @param badge         徽标文字
 * @param badgeType     徽标类型
 * @param badgeVariants 徽标颜色
 */
public record MenuMeta(
        String title,
        String icon,
        Integer order,
        Boolean keepAlive,
        Boolean affixTab,
        Boolean hideInMenu,
        String link,
        String iframeSrc,
        String badge,
        String badgeType,
        String badgeVariants) {
}
