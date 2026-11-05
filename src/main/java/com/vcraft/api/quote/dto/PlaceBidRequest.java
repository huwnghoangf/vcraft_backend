package com.vcraft.api.quote.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PlaceBidRequest(
        @NotNull(message = "Đơn giá không được để trống")
        @DecimalMin(value = "0.0", inclusive = false, message = "Đơn giá phải lớn hơn 0")
        BigDecimal pricePerUnit,

        BigDecimal totalPrice,

        @Min(value = 1, message = "Thời gian ước tính tối thiểu là 1 ngày")
        int estimatedDays,

        String note,
        String deliveryTerms
) {}
