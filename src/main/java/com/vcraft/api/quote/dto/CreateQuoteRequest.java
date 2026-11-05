package com.vcraft.api.quote.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateQuoteRequest(
        @NotBlank(message = "Tiêu đề yêu cầu báo giá không được để trống")
        String title,

        @NotBlank(message = "Mô tả chi tiết sản phẩm cần đặt không được để trống")
        String description,

        @Min(value = 1, message = "Số lượng đặt tối thiểu là 1")
        int quantity,

        String unit,
        Instant deadline,
        UUID categoryId,
        List<UUID> targetArtisanIds
) {}
