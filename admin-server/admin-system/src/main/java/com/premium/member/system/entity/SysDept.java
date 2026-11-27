package com.premium.member.system.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 组织部门。
 */
@Data
@TableName("sys_dept")
public class SysDept {

    /** 部门 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父部门 ID，顶级为 0。 */
    private Long parentId;

    /** 部门名称。 */
    private String name;

    /** 负责人。 */
    private String leader;

    /** 联系电话。 */
    private String phone;

    /** 排序值。 */
    private Integer sort;

    /** 状态：1 启用，0 停用。 */
    private Integer status;

    /** 创建时间。 */
    private OffsetDateTime createdAt;

    /** 更新时间。 */
    private OffsetDateTime updatedAt;

    /** 逻辑删除：1 已删除，0 未删除。 */
    @TableLogic
    private Integer deleted;
}
