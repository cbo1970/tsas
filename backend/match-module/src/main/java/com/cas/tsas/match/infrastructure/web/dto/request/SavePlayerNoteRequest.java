package com.cas.tsas.match.infrastructure.web.dto.request;

import jakarta.validation.constraints.Size;

public record SavePlayerNoteRequest(@Size(max = 2000) String note) {}
