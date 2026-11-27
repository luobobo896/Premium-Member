package com.premium.member.system.dto;

import java.util.List;

/**
 * 管理端菜单树节点，包含按钮和权限标识。
 *
 * @param id         菜单 ID
 * @param parentId   父 ID
 * @param name       路由 name
 * @param path       路由 path
 * @param component  组件路径
 * @param redirect   重定向
 * @param type       catalog/menu/button/embedded/link
 * @param title      标题
 * @param icon       图标
 * @param authCode   按钮权限标识
 * @param sort       排序
 * @param status     状态：1 启用，0 停用
 * @param visible    是否可见：1 可见，0 隐藏
 * @param children   子节点
 */
public record MenuNode(
        Long id,
        Long parentId,
        String name,
        String path,
        String component,
        String redirect,
        String type,
        String title,
        String icon,
        String authCode,
        Integer sort,
        Integer status,
        Integer visible,
        List<MenuNode> children) {
}
