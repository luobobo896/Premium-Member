package com.premium.member.system.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 系统用户。
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 用户 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名，未删除用户内唯一。 */
    private String username;

    /** BCrypt 密码哈希。 */
    private String passwordHash;

    /** 展示昵称。 */
    private String realName;

    /** 头像 URL。 */
    private String avatar;

    /** 用户描述。 */
    private String description;

    /** 登录后首页。 */
    private String homePath;

    /** 邮箱。 */
    private String email;

    /** 手机号。 */
    private String phone;

    /** 所属部门 ID。 */
    private Long deptId;

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
