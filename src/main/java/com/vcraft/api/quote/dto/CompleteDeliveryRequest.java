package com.vcraft.api.quote.dto;

import java.util.List;

public record CompleteDeliveryRequest(
        String deliveryNote,
        List<String> deliveryImages
) {}
