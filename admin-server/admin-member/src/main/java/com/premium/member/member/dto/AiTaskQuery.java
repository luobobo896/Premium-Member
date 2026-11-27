package com.premium.member.member.dto;

import com.premium.member.common.model.PageQuery;

/**
 * AI 任务分页条件。
 */
public class AiTaskQuery extends PageQuery {

    private String status;
    private String type;
    private Long memberId;
    private String taskNo;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }
}
