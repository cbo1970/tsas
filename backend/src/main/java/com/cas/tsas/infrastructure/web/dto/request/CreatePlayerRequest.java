package com.cas.tsas.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePlayerRequest(
        @NotBlank String name,
        String gender,
        Integer ranking,
        String handedness,
        String backhandType
) {}
