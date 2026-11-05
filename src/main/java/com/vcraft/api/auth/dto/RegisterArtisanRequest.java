package com.vcraft.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record RegisterArtisanRequest(
        @NotBlank(message = "Tên xưởng không được để trống")
        String workshopName,

        @NotBlank(message = "Tên chủ xưởng không được để trống")
        String ownerName,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        String phone,

        @NotBlank(message = "Địa chỉ xưởng không được để trống")
        String address,

        Double lat,
        Double lng,
        String description,
        List<String> certificates,
        String password
) {}
