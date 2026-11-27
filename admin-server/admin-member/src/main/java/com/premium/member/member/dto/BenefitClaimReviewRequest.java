package com.premium.member.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 人工核实/补偿参数。原因必填，并随审计日志落库。
 */
public record BenefitClaimReviewRequest(
        @NotBlank @Pattern(regexp = "SUCCESS|FAILED|COMPENSATED", message = "结果只能是 SUCCESS/FAILED/COMPENSATED")
        String result,
        @NotBlank @Size(max = 200) String reason) {
}
