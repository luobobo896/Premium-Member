package com.premium.member.member.dto;

import java.time.LocalDate;

import com.premium.member.common.model.PageQuery;

/**
 * 工单分页条件。
 */
public class TicketQuery extends PageQuery {

    private String status;
    private String category;
    private String keyword;
    private LocalDate from;
    private LocalDate to;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }
}
