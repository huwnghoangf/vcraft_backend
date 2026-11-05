package com.vcraft.domain.service;

import com.vcraft.domain.model.User;
import java.time.Instant;

public interface TokenIssuer {
    String issueAccessToken(User user);
    String generateRefreshToken();
    Instant getRefreshTokenExpiry();
}
