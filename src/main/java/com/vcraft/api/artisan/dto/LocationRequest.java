package com.vcraft.api.artisan.dto;

public record LocationRequest(
        Double lat,
        Double lng,
        String address
) {}
