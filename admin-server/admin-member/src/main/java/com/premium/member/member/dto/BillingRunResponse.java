package com.premium.member.member.dto;

/**
 * 一次任务执行摘要。
 */
public record BillingRunResponse(
        String period,
        int processed,
        int granted,
        int failed,
        int suspended) {
}
