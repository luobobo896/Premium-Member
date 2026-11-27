package com.premium.member.system.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 接口/按钮权限码。
 */
@Data
@TableName("sys_permission")
public class SysPermission {

    /** 权限 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权限码，如 member:list。 */
    private String code;

    /** 权限名称。 */
    private String name;

    /** 类型：API 或 BUTTON。 */
    private String type;

    /** 说明。 */
    private String description;

    /** 创建时间。 */
    private OffsetDateTime createdAt;

    /** 更新时间。 */
    private OffsetDateTime updatedAt;

    /** 逻辑删除：1 已删除，0 未删除。 */
    @TableLogic
    private Integer deleted;
}
