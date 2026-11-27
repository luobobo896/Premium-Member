package com.premium.member.member.dto;

/**
 * 手动执行订阅任务。只用于验收/补偿，正式环境由定时任务执行。
 */
public record BillingRunRequest(
        Long subscriptionId,
        String period,
        String mockResult,
        Integer graceDays,
        Boolean forceSuspend) {
}
