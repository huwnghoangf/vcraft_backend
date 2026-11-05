package com.vcraft.api.auth.dto;

import com.vcraft.domain.model.Role;
import com.vcraft.domain.model.User;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserInfo user
) {
    public record UserInfo(
            UUID id,
            String email,
            String fullName,
            String phone,
            Role role
    ) {
        public static UserInfo from(User user) {
            return new UserInfo(user.id(), user.email(), user.fullName(), user.phone(), user.role());
        }
    }

    public static AuthResponse of(String accessToken, String refreshToken, User user) {
        return new AuthResponse(accessToken, refreshToken, UserInfo.from(user));
    }
}
