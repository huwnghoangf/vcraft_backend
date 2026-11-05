package com.vcraft.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Cart(
        UUID id,
        UUID userId,
        List<CartItem> items,
        Instant createdAt
) {}
