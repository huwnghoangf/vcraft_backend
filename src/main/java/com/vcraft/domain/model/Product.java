package com.vcraft.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Product(
        UUID id,
        UUID artisanId,
        UUID categoryId,
        UUID batchId,
        String name,
        String description,
        BigDecimal price,
        int stock,
        List<String> images,
        Instant createdAt,
        Instant updatedAt
) {}
