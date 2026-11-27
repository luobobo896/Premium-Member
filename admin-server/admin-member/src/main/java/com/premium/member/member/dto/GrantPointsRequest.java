package com.premium.member.member.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GrantPointsRequest(
        @NotNull @Min(1) Integer points,
        @NotBlank @Size(max = 200) String reason) {
}
