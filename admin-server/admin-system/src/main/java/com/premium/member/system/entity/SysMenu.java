package com.premium.member.system.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 菜单/路由/按钮。
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    /** 菜单 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父菜单 ID，顶级为 0。 */
    private Long parentId;

    /** 路由 name，vben 必填。 */
    private String name;

    /** 路由 path。 */
    private String path;

    /** 前端组件路径，按钮为空。 */
    private String component;

    /** 重定向路径。 */
    private String redirect;

    /** 类型：catalog/menu/button/embedded/link。 */
    private String type;

    /** 菜单标题。 */
    private String title;

    /** 图标标识。 */
    private String icon;

    /** 按钮/接口权限码。 */
    private String permissionCode;

    /** 排序值。 */
    private Integer sort;

    /** 状态：1 启用，0 停用。 */
    private Integer status;

    /** 是否可见：1 可见，0 隐藏。 */
    private Integer visible;

    /** 是否 KeepAlive：1 是，0 否。 */
    private Integer keepAlive;

    /** 是否固定标签页：1 是，0 否。 */
    private Integer affixTab;

    /** 是否在菜单隐藏：1 是，0 否。 */
    private Integer hideInMenu;

    /** 外链地址。 */
    private String link;

    /** iframe 地址。 */
    private String iframeSrc;

    /** 徽标文字。 */
    private String badge;

    /** 徽标类型：dot/normal。 */
    private String badgeType;

    /** 徽标颜色。 */
    private String badgeVariants;

    /** 创建时间。 */
    private OffsetDateTime createdAt;

    /** 更新时间。 */
    private OffsetDateTime updatedAt;

    /** 逻辑删除：1 已删除，0 未删除。 */
    @TableLogic
    private Integer deleted;
}
