package com.premium.member.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 退款审核。APPROVE 才会真正退款并回收周期。
 */
public record RefundRequest(
        @NotNull @Min(1) Long amountCents,
        @NotBlank @Size(max = 255) String reason,
        @NotBlank @Pattern(regexp = "APPROVE|REJECT") String result) {
}
