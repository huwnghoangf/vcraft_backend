package com.vcraft.api.artisan.dto;

import java.util.List;

public record UpdateArtisanRequest(
        String workshopName,
        String description,
        String address,
        Double lat,
        Double lng,
        Boolean openStatus,
        List<String> images,
        String familyHistory,
        List<String> certificates
) {}
