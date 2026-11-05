package com.vcraft.api.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank(message = "Tên sản phẩm gốm không được để trống")
        String name,

        String description,

        @NotNull(message = "Giá sản phẩm không được để trống")
        @DecimalMin(value = "0.0", inclusive = false, message = "Giá sản phẩm phải lớn hơn 0")
        BigDecimal price,

        @Min(value = 0, message = "Số lượng tồn kho không được âm")
        int stock,

        UUID categoryId,
        UUID batchId,
        List<String> images
) {}
