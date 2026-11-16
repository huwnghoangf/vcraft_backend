package com.vcraft.domain.service;

import com.vcraft.domain.exception.*;
import com.vcraft.domain.model.User;
import com.vcraft.domain.repository.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

public class PasswordResetService {
    private final UserRepository users;
    private final PasswordResetRepository tokens;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHasher hasher;
    private final PasswordResetDelivery delivery;
    private final Clock clock;
    public PasswordResetService(UserRepository users, PasswordResetRepository tokens, RefreshTokenRepository refreshTokens,
                                PasswordHasher hasher, PasswordResetDelivery delivery, Clock clock) {
        this.users = users; this.tokens = tokens; this.refreshTokens = refreshTokens;
        this.hasher = hasher; this.delivery = delivery; this.clock = clock;
    }
    /** Same response for unknown, blocked, and throttled accounts. No token in the API response. */
    public void request(String email) {
        if (!delivery.available()) throw new ServiceUnavailableException("Chưa cấu hình dịch vụ email đặt lại mật khẩu");
        var found = users.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (found.isEmpty()) return;
        var locked = users.findByIdForUpdate(found.get().id());
        if (locked.isEmpty() || !locked.get().isActive()) return;
        User user = locked.get();
        Instant now = clock.instant();
        if (tokens.issuedSince(user.id(), now.minusSeconds(60))) return;
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.invalidateAll(user.id(), now);
        tokens.save(new PasswordResetRepository.Token(hash(raw), user.id(), now, now.plus(Duration.ofMinutes(15))));
        delivery.send(user.email(), raw);
    }
    public void reset(String raw, String password) {
        if (raw == null || !raw.matches("[A-Za-z0-9_-]{43}") || password == null || password.length() < 6
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new InvalidInputException("Token hoặc mật khẩu không hợp lệ");
        var token = tokens.find(hash(raw)).orElseThrow(() -> new InvalidInputException("Token không hợp lệ hoặc đã hết hạn"));
        User user = users.findByIdForUpdate(token.userId()).filter(User::isActive)
                .orElseThrow(() -> new InvalidInputException("Token không hợp lệ hoặc đã hết hạn"));
        Instant now = clock.instant();
        if (!token.expiresAt().isAfter(now) || !tokens.consume(token.hash(), now))
            throw new InvalidInputException("Token không hợp lệ hoặc đã hết hạn");
        users.save(new User(user.id(), user.email(), hasher.hash(password), user.fullName(), user.phone(), user.role(),
                user.status(), user.createdAt(), now, user.securityVersion() + 1));
        tokens.invalidateAll(user.id(), now);
        refreshTokens.deleteByUserId(user.id());
    }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
