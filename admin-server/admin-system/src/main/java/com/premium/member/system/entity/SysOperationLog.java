package com.premium.member.system.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 操作审计日志。
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    /** 日志 ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人 ID，登录失败时为空。 */
    private Long userId;

    /** 操作人登录名。 */
    private String username;

    /** 模块名。 */
    private String module;

    /** 操作名。 */
    private String action;

    /** HTTP 方法。 */
    private String method;

    /** 请求路径。 */
    private String path;

    /** 脱敏后的请求参数。 */
    private String params;

    /** 客户端 IP。 */
    private String ip;

    /** User-Agent，最长 512。 */
    private String userAgent;

    /** 是否成功：1 成功，0 失败。 */
    private Integer success;

    /** 失败时的业务错误码。 */
    private String errorCode;

    /** 耗时，毫秒。 */
    private Long costMs;

    /** 创建时间。 */
    private OffsetDateTime createdAt;
}
