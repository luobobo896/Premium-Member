package com.premium.member.member.dto;

import lombok.Data;

/**
 * AI 每日用量。
 */
@Data
public class AiUsageRow {

    private String day;
    private Long total;
    private Long success;
    private Long failed;
    private Long rejected;
    private Long unknown;
    private Long imageTasks;
    private Long videoTasks;
    private Long costCents;
}
