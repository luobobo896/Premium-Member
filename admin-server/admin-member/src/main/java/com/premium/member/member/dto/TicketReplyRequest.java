package com.premium.member.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 后台回复工单。
 */
public record TicketReplyRequest(@NotBlank @Size(max = 5000) String content) {
}
