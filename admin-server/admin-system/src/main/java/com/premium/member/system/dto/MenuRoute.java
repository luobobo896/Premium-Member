package com.premium.member.system.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * /menu/all 返回的 vben 路由节点。
 *
 * @param id        菜单 ID
 * @param name      路由 name
 * @param path      路由 path
 * @param component 组件路径，目录可为 null
 * @param redirect  重定向
 * @param meta      路由元信息
 * @param children  子路由
 */
public record MenuRoute(
        Long id,
        String name,
        String path,
        @JsonInclude(JsonInclude.Include.NON_NULL) String component,
        @JsonInclude(JsonInclude.Include.NON_NULL) String redirect,
        MenuMeta meta,
        List<MenuRoute> children) {
}
