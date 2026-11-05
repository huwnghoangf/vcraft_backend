package com.vcraft.api.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProductRequest(
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        List<String> images
) {}
