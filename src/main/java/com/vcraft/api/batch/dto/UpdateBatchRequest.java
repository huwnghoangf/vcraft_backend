package com.vcraft.api.batch.dto;

public record UpdateBatchRequest(
        String name,
        String description,
        String status
) {}
