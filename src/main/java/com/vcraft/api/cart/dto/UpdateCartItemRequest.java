package com.vcraft.api.cart.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(
        @Min(value = 0, message = "Số lượng không được âm")
        int quantity
) {}
