package com.premium.member.member.dto;

import com.premium.member.common.model.PageQuery;

/**
 * 支付流水分页条件。
 */
public class PaymentQuery extends PageQuery {

    private String channel;
    private String txnNo;
    private String orderNo;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getTxnNo() {
        return txnNo;
    }

    public void setTxnNo(String txnNo) {
        this.txnNo = txnNo;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
}
