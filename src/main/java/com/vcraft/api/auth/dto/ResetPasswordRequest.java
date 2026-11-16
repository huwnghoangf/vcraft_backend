package com.vcraft.api.auth.dto;
import jakarta.validation.constraints.*;
public record ResetPasswordRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
                                   @NotBlank @Size(min = 6, max = 72) String newPassword) {}
