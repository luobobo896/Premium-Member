package com.premium.member.system.dto;

import java.time.OffsetDateTime;

import com.premium.member.common.model.PageQuery;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 操作日志分页查询条件。
 */
public class OperationLogQuery extends PageQuery {

    /** 操作人登录名模糊匹配。 */
    private String username;

    /** 模块精确匹配。 */
    private String module;

    /** 开始时间，ISO-8601。 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime startTime;

    /** 结束时间，ISO-8601。 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime endTime;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
    }
}
