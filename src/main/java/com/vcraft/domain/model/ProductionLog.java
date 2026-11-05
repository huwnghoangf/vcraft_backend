package com.vcraft.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductionLog(
        UUID id,
        UUID batchId,
        String stage,
        String note,
        List<String> images,
        Instant loggedAt,
        Instant createdAt
) {}
