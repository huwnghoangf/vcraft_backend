package com.vcraft.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vcraft.domain.exception.ConflictException;
import com.vcraft.domain.exception.InvalidInputException;
import com.vcraft.domain.exception.NotFoundException;
import com.vcraft.domain.exception.UnauthorizedException;
import com.vcraft.domain.model.Role;
import com.vcraft.domain.model.User;
import com.vcraft.domain.model.UserStatus;
import com.vcraft.domain.repository.ArtisanRepository;
import com.vcraft.domain.repository.RefreshTokenRepository;
import com.vcraft.domain.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final User ACTIVE_USER = new User(
            USER_ID,
            "buyer@vcraft.vn",
            "$2a$10$hashedpassword",
            "Nguyễn Văn A",
            "0912345678",
            Role.BUYER,
            UserStatus.ACTIVE,
            Instant.now(),
            Instant.now()
    );

    private static final User BANNED_USER = new User(
            UUID.randomUUID(),
            "banned@vcraft.vn",
            "$2a$10$hashedpassword",
            "Người Dùng Vi Phạm",
            "0912345679",
            Role.BUYER,
            UserStatus.BLOCKED,
            Instant.now(),
            Instant.now()
    );

    private UserRepository userRepository;
    private ArtisanRepository artisanRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordHasher passwordHasher;
    private TokenIssuer tokenIssuer;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        artisanRepository = mock(ArtisanRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordHasher = mock(PasswordHasher.class);
        tokenIssuer = mock(TokenIssuer.class);

        authService = new AuthService(
                userRepository,
                artisanRepository,
                refreshTokenRepository,
                passwordHasher,
                tokenIssuer
        );
    }

    // ====== Đăng ký ======

    @Test
    @DisplayName("Đăng ký Buyer với email đã tồn tại phải ném ConflictException")
    void registerBuyerWithDuplicateEmailThrowsConflict() {
        when(userRepository.existsByEmail("buyer@vcraft.vn")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerBuyer("Nguyễn Văn A", "buyer@vcraft.vn", "secret123", "0912345678"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email đã được đăng ký");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đăng ký Artisan với email đã tồn tại phải ném ConflictException")
    void registerArtisanWithDuplicateEmailThrowsConflict() {
        when(userRepository.existsByEmail("artisan@vcraft.vn")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerArtisan(
                "Xưởng Gốm Minh Long", "Trần Văn B", "artisan@vcraft.vn",
                "0987654321", "Thôn 1 Bát Tràng", 20.97, 105.91,
                "Chuyên ấm chén", null, "secret123"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email đã được đăng ký");

        verify(userRepository, never()).save(any());
        verify(artisanRepository, never()).save(any());
    }

    // ====== Đăng nhập ======

    @Test
    @DisplayName("Đăng nhập với email không tồn tại phải ném UnauthorizedException")
    void loginWithNonExistentEmailThrowsUnauthorized() {
        when(userRepository.findByEmail("unknown@vcraft.vn")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("unknown@vcraft.vn", "password123"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("không chính xác");
    }

    @Test
    @DisplayName("Đăng nhập với mật khẩu sai phải ném UnauthorizedException")
    void loginWithWrongPasswordThrowsUnauthorized() {
        when(userRepository.findByEmail("buyer@vcraft.vn")).thenReturn(Optional.of(ACTIVE_USER));
        when(passwordHasher.matches("wrongpassword", ACTIVE_USER.passwordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login("buyer@vcraft.vn", "wrongpassword"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("không chính xác");

        verify(tokenIssuer, never()).issueAccessToken(any());
    }

    @Test
    @DisplayName("Đăng nhập khi tài khoản bị khóa (BANNED) phải ném UnauthorizedException")
    void loginWithBannedAccountThrowsUnauthorized() {
        when(userRepository.findByEmail("banned@vcraft.vn")).thenReturn(Optional.of(BANNED_USER));

        assertThatThrownBy(() -> authService.login("banned@vcraft.vn", "secret123"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("đã bị khóa");

        verify(tokenIssuer, never()).issueAccessToken(any());
    }

    // ====== Refresh Token ======

    @Test
    @DisplayName("Refresh token thành công phải thực hiện xoay vòng (Token Rotation): hủy token cũ và lưu token mới")
    void refreshTokenRotatesTokensSuccessfully() {
        String oldToken = "valid-old-token";
        when(refreshTokenRepository.findUserIdByToken(oldToken)).thenReturn(Optional.of(USER_ID));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(ACTIVE_USER));
        when(tokenIssuer.issueAccessToken(ACTIVE_USER)).thenReturn("new-access-token");
        when(tokenIssuer.generateRefreshToken()).thenReturn("new-refresh-token");
        when(tokenIssuer.getRefreshTokenExpiry()).thenReturn(Instant.now().plusSeconds(86400));

        AuthService.AuthResult result = authService.refreshToken(oldToken);

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");

        // Kiểm tra bắt buộc phải xóa token cũ không được dùng lại
        verify(refreshTokenRepository).deleteByToken(oldToken);
        // Kiểm tra lưu token mới
        verify(refreshTokenRepository).save(eq(USER_ID), eq("new-refresh-token"), any());
    }

    @Test
    @DisplayName("Refresh token không hợp lệ hoặc không tồn tại phải ném UnauthorizedException")
    void refreshTokenNotFoundThrowsUnauthorized() {
        when(refreshTokenRepository.findUserIdByToken("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken("invalid-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Refresh token không hợp lệ hoặc đã hết hạn");
    }

    @Test
    @DisplayName("Refresh token của tài khoản bị khóa phải ném UnauthorizedException")
    void refreshTokenForBannedUserThrowsUnauthorized() {
        when(refreshTokenRepository.findUserIdByToken("valid-token")).thenReturn(Optional.of(BANNED_USER.id()));
        when(userRepository.findById(BANNED_USER.id())).thenReturn(Optional.of(BANNED_USER));

        assertThatThrownBy(() -> authService.refreshToken("valid-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Tài khoản đã bị khóa");
    }

    // ====== Thông tin tài khoản & Đổi mật khẩu ======

    @Test
    @DisplayName("getMe với userId không tồn tại phải ném NotFoundException")
    void getMeNonExistentUserThrowsNotFound() {
        UUID nonExistent = UUID.randomUUID();
        when(userRepository.findById(nonExistent)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getMe(nonExistent))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Không tìm thấy thông tin tài khoản");
    }

    @Test
    @DisplayName("Đổi mật khẩu với mật khẩu cũ sai phải ném InvalidInputException")
    void changePasswordWithWrongOldPasswordThrowsInvalidInput() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(ACTIVE_USER));
        when(passwordHasher.matches("wrong-old", ACTIVE_USER.passwordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(USER_ID, "wrong-old", "new-secret"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("Mật khẩu hiện tại không chính xác");

        verify(userRepository, never()).save(any());
    }
}
