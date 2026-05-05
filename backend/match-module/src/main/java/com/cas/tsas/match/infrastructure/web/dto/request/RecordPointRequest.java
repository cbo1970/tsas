package com.cas.tsas.match.infrastructure.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RecordPointRequest(
        @NotNull @Min(1) @Max(2) Integer winner,
        String pointType,
        String strokeType,
        String direction,
        String remark
) {}
