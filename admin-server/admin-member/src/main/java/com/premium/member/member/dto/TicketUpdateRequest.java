package com.premium.member.member.dto;

import jakarta.validation.constraints.Size;

/**
 * 工单状态、优先级、处理人与备注。
 */
public record TicketUpdateRequest(
        String status,
        String priority,
        @Size(max = 1000) String remark,
        Long assigneeAdminId) {
}
