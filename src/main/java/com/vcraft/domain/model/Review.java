package com.vcraft.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Review(
        UUID id,
        UUID userId,
        UUID productId,
        UUID artisanId,
        int rating,
        String comment,
        List<String> images,
        Instant createdAt,
        Instant updatedAt
) {}
