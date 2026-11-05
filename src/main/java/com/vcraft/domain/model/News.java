package com.vcraft.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record News(
        UUID id,
        UUID authorId,
        String title,
        String content,
        NewsType type,
        List<String> images,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt
) {}
