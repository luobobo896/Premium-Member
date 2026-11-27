package com.premium.member.c.biz.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * C 端提交工单。
 */
public record CTicketCreateRequest(
        @NotBlank @Size(max = 128) String title,
        @NotBlank @Size(max = 5000) String content,
        @Size(max = 32) String category,
        @Size(max = 40) String orderNo,
        @Size(max = 16) String priority) {
}
