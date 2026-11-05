package com.vcraft.api.batch.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record CreateBatchRequest(
        @NotBlank(message = "Tên lô sản xuất không được để trống")
        String name,
        String description,
        int quantity,
        List<UUID> productIds
) {}
