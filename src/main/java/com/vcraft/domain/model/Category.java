package com.vcraft.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Category(
        UUID id,
        String name,
        String description,
        String icon,
        Instant createdAt
) {}
