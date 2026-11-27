package com.premium.member.member.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * 卡密批量导入参数。
 */
public record BenefitCardImportRequest(
        @NotBlank @Size(max = 64) String batchNo,
        @NotBlank @Size(max = 64) String productSku,
        @NotEmpty @Size(max = 1000) @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        List<@NotBlank @Size(max = 128) String> codes,
        OffsetDateTime expireAt) {
}
