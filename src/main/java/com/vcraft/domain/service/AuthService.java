package com.vcraft.domain.service;

import com.vcraft.domain.exception.ConflictException;
import com.vcraft.domain.exception.InvalidInputException;
import com.vcraft.domain.exception.NotFoundException;
import com.vcraft.domain.exception.UnauthorizedException;
import com.vcraft.domain.model.ArtisanProfile;
import com.vcraft.domain.model.ArtisanStatus;
import com.vcraft.domain.model.Role;
import com.vcraft.domain.model.User;
import com.vcraft.domain.model.UserStatus;
import com.vcraft.domain.repository.ArtisanRepository;
import com.vcraft.domain.repository.RefreshTokenRepository;
import com.vcraft.domain.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AuthService {

    public record AuthResult(User user, String accessToken, String refreshToken) {}

    private final UserRepository userRepository;
    private final ArtisanRepository artisanRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public AuthService(UserRepository userRepository,
                       ArtisanRepository artisanRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordHasher passwordHasher,
                       TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.artisanRepository = artisanRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    public AuthResult registerBuyer(String name, String email, String rawPassword, String phone) {
        email = email.toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email đã được đăng ký trong hệ thống");
        }
        validatePassword(rawPassword);
        User user = new User(
                UUID.randomUUID(),
                email.toLowerCase().trim(),
                passwordHasher.hash(rawPassword),
                name,
                phone,
                Role.BUYER,
                UserStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );
        User saved = userRepository.save(user);

        String accessToken = tokenIssuer.issueAccessToken(saved);
        String refreshToken = tokenIssuer.generateRefreshToken();
        refreshTokenRepository.save(saved.id(), refreshToken, tokenIssuer.getRefreshTokenExpiry());

        return new AuthResult(saved, accessToken, refreshToken);
    }

    public ArtisanProfile registerArtisan(String workshopName, String ownerName, String email,
                                          String phone, String address, Double lat, Double lng,
                                          String description, List<String> certificates, String rawPassword) {
        if ((lat != null && (!Double.isFinite(lat) || lat < -90 || lat > 90))
                || (lng != null && (!Double.isFinite(lng) || lng < -180 || lng > 180)))
            throw new InvalidInputException("Tọa độ xưởng không hợp lệ");
        email = email.toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email đã được đăng ký trong hệ thống");
        }

        validatePassword(rawPassword);
        User user = new User(
                UUID.randomUUID(),
                email.toLowerCase().trim(),
                passwordHasher.hash(rawPassword),
                ownerName,
                phone,
                Role.ARTISAN,
                UserStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );
        User savedUser = userRepository.save(user);

        ArtisanProfile profile = new ArtisanProfile(
                UUID.randomUUID(),
                savedUser.id(),
                workshopName,
                ownerName,
                phone,
                address,
                "Bát Tràng",
                lat,
                lng,
                false,
                description,
                null,
                certificates != null ? certificates : List.of(),
                List.of(),
                ArtisanStatus.PENDING,
                null,
                Instant.now(),
                Instant.now()
        );
        return artisanRepository.save(profile);
    }

    public AuthResult login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new UnauthorizedException("Email hoặc mật khẩu không chính xác"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ BQL làng nghề.");
        }

        if (!passwordHasher.matches(rawPassword, user.passwordHash())) {
            throw new UnauthorizedException("Email hoặc mật khẩu không chính xác");
        }

        String accessToken = tokenIssuer.issueAccessToken(user);
        String refreshToken = tokenIssuer.generateRefreshToken();
        refreshTokenRepository.save(user.id(), refreshToken, tokenIssuer.getRefreshTokenExpiry());

        return new AuthResult(user, accessToken, refreshToken);
    }

    public AuthResult refreshToken(String token) {
        UUID userId = refreshTokenRepository.findUserIdByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Refresh token không hợp lệ hoặc đã hết hạn"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Người dùng không tồn tại"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Tài khoản đã bị khóa");
        }

        // Token Rotation: hủy refresh token cũ ngay khi sử dụng và sinh cặp token mới
        refreshTokenRepository.deleteByToken(token);

        String newAccessToken = tokenIssuer.issueAccessToken(user);
        String newRefreshToken = tokenIssuer.generateRefreshToken();
        refreshTokenRepository.save(user.id(), newRefreshToken, tokenIssuer.getRefreshTokenExpiry());

        return new AuthResult(user, newAccessToken, newRefreshToken);
    }

    public void revokeRefreshToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    public User getMe(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin tài khoản"));
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank() || password.length() < 6
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new InvalidInputException("Mật khẩu cần ít nhất 6 ký tự, tối đa 72 byte UTF-8");
    }
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        validatePassword(newPassword);
        User user = userRepository.findByIdForUpdate(userId).orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản"));
        if (!passwordHasher.matches(oldPassword, user.passwordHash())) {
            throw new InvalidInputException("Mật khẩu hiện tại không chính xác");
        }

        User updated = new User(
                user.id(),
                user.email(),
                passwordHasher.hash(newPassword),
                user.fullName(),
                user.phone(),
                user.role(),
                user.status(),
                user.createdAt(),
                Instant.now(),
                user.securityVersion() + 1
        );
        userRepository.save(updated);
        refreshTokenRepository.deleteByUserId(userId);
    }
}
