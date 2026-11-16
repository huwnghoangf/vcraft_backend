package com.vcraft.api.quote.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record UpdateBidRequest(@DecimalMin(value="0", inclusive=false) @Digits(integer=13,fraction=2) BigDecimal pricePerUnit,
                               @Min(1) Integer estimatedDays, String note) {}
