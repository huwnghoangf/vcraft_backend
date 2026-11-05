package com.vcraft.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Batch(
        UUID id,
        UUID artisanId,
        String name,
        String description,
        int quantity,
        BatchStatus status,
        String qrCodeUrl,
        String qrToken,
        Instant createdAt,
        Instant updatedAt
) {}
