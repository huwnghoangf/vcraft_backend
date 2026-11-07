package com.vcraft.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.vcraft.domain.model.Role;
import com.vcraft.domain.model.User;
import com.vcraft.domain.model.UserStatus;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;

class JwtTokenProviderTest {

    private static final String SECRET_1 = "vcraft-bat-trang-secret-key-32chars-minimum-token";
    private static final String SECRET_2 = "another-secret-key-32chars-minimum-different-key";

    private final JwtProperties properties = new JwtProperties(SECRET_1, 60, 7);
    private final JwtTokenProvider provider = new JwtTokenProvider(properties);

    private final User user = new User(
            UUID.randomUUID(),
            "artisan@vcraft.vn",
            "hashed",
            "Nghệ Nhân Bát Tràng",
            "0912345678",
            Role.ARTISAN,
            UserStatus.ACTIVE,
            Instant.now(),
            Instant.now()
    );

    @Test
    @DisplayName("Token hợp lệ phân tích ra đúng userId, email, role")
    void validTokenParsesCorrectClaims() {
        String token = provider.issueAccessToken(user);

        Optional<Claims> claimsOpt = provider.parseClaims(token);
        assertThat(claimsOpt).isPresent();

        Claims claims = claimsOpt.get();
        assertThat(claims.getSubject()).isEqualTo(user.id().toString());
        assertThat(claims.get("email", String.class)).isEqualTo("artisan@vcraft.vn");
        assertThat(claims.get("role", String.class)).isEqualTo("ARTISAN");
    }

    @Test
    @DisplayName("Chuỗi không phải JWT khi parse phải trả về Optional.empty")
    void invalidJwtStringReturnsEmpty() {
        assertThat(provider.parseClaims("not-a-valid-jwt-token")).isEmpty();
        assertThat(provider.parseClaims("")).isEmpty();
        assertThat(provider.parseClaims(null)).isEmpty();
    }

    @Test
    @DisplayName("Token bị can thiệp/sửa đổi chữ ký phải bị từ chối (trả về Optional.empty)")
    void tamperedTokenIsRejected() {
        String token = provider.issueAccessToken(user);

        // Đổi ký tự cuối của chữ ký
        int lastIndex = token.length() - 1;
        char tamperedChar = token.charAt(lastIndex) == 'a' ? 'b' : 'a';
        String tamperedToken = token.substring(0, lastIndex) + tamperedChar;

        assertThat(provider.parseClaims(tamperedToken)).isEmpty();
    }

    @Test
    @DisplayName("Token ký bằng secret key khác phải bị từ chối")
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtTokenProvider anotherProvider = new JwtTokenProvider(new JwtProperties(SECRET_2, 60, 7));
        String token = anotherProvider.issueAccessToken(user);

        // provider dùng SECRET_1 để parse token của SECRET_2
        assertThat(provider.parseClaims(token)).isEmpty();
    }

    @Test
    @DisplayName("Token đã hết hạn trong quá khứ phải bị từ chối")
    void expiredTokenIsRejected() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET_1.getBytes(StandardCharsets.UTF_8));
        Instant past = Instant.now().minus(Duration.ofMinutes(10));
        String expiredToken = Jwts.builder()
                .subject(user.id().toString())
                .claim("email", user.email())
                .claim("role", user.role().name())
                .issuedAt(Date.from(past.minus(Duration.ofMinutes(60))))
                .expiration(Date.from(past))
                .signWith(key)
                .compact();

        assertThat(provider.parseClaims(expiredToken)).isEmpty();
    }
}
