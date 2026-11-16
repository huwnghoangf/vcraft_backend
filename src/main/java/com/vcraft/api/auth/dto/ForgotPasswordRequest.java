package com.vcraft.api.auth.dto;
import jakarta.validation.constraints.*;
public record ForgotPasswordRequest(@NotBlank @Email String email) {}
