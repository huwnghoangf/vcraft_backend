package com.vcraft.api.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddToCartRequest(
        @NotNull(message = "ID sản phẩm không được để trống")
        UUID productId,

        @Min(value = 1, message = "Số lượng tối thiểu là 1")
        int quantity
) {}
