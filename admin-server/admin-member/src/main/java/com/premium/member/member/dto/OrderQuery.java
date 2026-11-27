package com.premium.member.member.dto;

import com.premium.member.common.model.PageQuery;

/**
 * 订单分页条件。
 */
public class OrderQuery extends PageQuery {

    private String status;
    private String orderNo;
    private Long memberId;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
}
