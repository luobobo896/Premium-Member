package com.premium.member.member.dto;

import lombok.Data;

/**
 * 每日运营统计。
 */
@Data
public class DailyStatRow {

    private String day;
    private Long orderCount;
    private Long paidAmountCents;
    private Long activeMembers;
    private Long benefitApplied;
    private Long benefitSuccess;
    private Long benefitException;
    private Long aiImageTasks;
    private Long aiVideoTasks;
}
