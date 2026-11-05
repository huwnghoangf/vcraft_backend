package com.vcraft.api.quote.dto;

import java.time.Instant;

public record UpdateQuoteRequest(
        String title,
        String description,
        Integer quantity,
        Instant deadline
) {}
