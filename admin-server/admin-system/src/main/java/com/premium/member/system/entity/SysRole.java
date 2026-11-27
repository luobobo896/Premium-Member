package com.premium.member.system.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 系统角色。
 */
@Data
@TableName("sys_role")
public class SysRole {

    /** 角色 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色码，前端 roles 字段使用。 */
    private String code;

    /** 角色名称。 */
    private String name;

    /** 数据范围：ALL/DEPT_AND_CHILD/DEPT/SELF。 */
    private String dataScope;

    /** 状态：1 启用，0 停用。 */
    private Integer status;

    /** 排序值。 */
    private Integer sort;

    /** 备注。 */
    private String remark;

    /** 创建时间。 */
    private OffsetDateTime createdAt;

    /** 更新时间。 */
    private OffsetDateTime updatedAt;

    /** 逻辑删除：1 已删除，0 未删除。 */
    @TableLogic
    private Integer deleted;
}
