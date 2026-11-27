package com.premium.member.member.dto;

import java.time.OffsetDateTime;

import lombok.Data;

/**
 * B 端 AI 任务列表行。
 */
@Data
public class AiTaskRow {

    private Long id;
    private String taskNo;
    private Long memberId;
    private String memberNo;
    private String memberName;
    private String phone;
    private Long periodId;
    private String periodNo;
    private Long templateId;
    private String templateCode;
    private String templateName;
    private String type;
    private String status;
    private String supplierTaskNo;
    private String failReason;
    private Long costCents;
    private OffsetDateTime createdAt;
    private OffsetDateTime finishedAt;
}
