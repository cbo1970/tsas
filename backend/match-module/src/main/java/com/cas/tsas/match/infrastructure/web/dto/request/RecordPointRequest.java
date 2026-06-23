package com.cas.tsas.match.infrastructure.web.dto.request;

import com.cas.tsas.match.domain.model.Direction;
import com.cas.tsas.match.domain.model.PointType;
import com.cas.tsas.match.domain.model.StrokeType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecordPointRequest(
        @NotNull @Min(1) @Max(2) Integer winner,
        PointType pointType,
        StrokeType strokeType,
        Direction direction,
        @Size(max = 500) String remark,
        @Min(1) @Max(2) Integer serveAttempt
) {}
