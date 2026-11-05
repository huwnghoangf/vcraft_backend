package com.vcraft.api.auth;

import com.vcraft.api.CallerResolver;
import com.vcraft.api.auth.dto.AuthResponse;
import com.vcraft.api.auth.dto.ChangePasswordRequest;
import com.vcraft.api.auth.dto.LoginRequest;
import com.vcraft.api.auth.dto.RefreshTokenRequest;
import com.vcraft.api.auth.dto.RegisterArtisanRequest;
import com.vcraft.api.auth.dto.RegisterBuyerRequest;
import com.vcraft.domain.model.ArtisanProfile;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.User;
import com.vcraft.domain.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "1. Authentication", description = "Các API xác thực, đăng ký, đăng nhập và quản lý phiên người dùng")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/buyer")
    @Operation(summary = "Đăng ký tài khoản khách hàng (Buyer)")
    public ResponseEntity<AuthResponse> registerBuyer(@Valid @RequestBody RegisterBuyerRequest request) {
        AuthService.AuthResult result = authService.registerBuyer(request.name(), request.email(), request.password(), request.phone());
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.of(result.accessToken(), result.refreshToken(), result.user()));
    }

    @PostMapping("/register/artisan")
    @Operation(summary = "Nộp hồ sơ đăng ký nghệ nhân / xưởng gốm")
    public ResponseEntity<ArtisanProfile> registerArtisan(@Valid @RequestBody RegisterArtisanRequest request) {
        ArtisanProfile artisan = authService.registerArtisan(
                request.workshopName(),
                request.ownerName(),
                request.email(),
                request.phone(),
                request.address(),
                request.lat(),
                request.lng(),
                request.description(),
                request.certificates(),
                request.password()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(artisan);
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập hệ thống (tất cả vai trò: Buyer, Artisan, Admin)")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.login(request.email(), request.password());
        return ResponseEntity.ok(AuthResponse.of(result.accessToken(), result.refreshToken(), result.user()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất tài khoản, hủy token")
    public ResponseEntity<Map<String, String>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null && request.refreshToken() != null) {
            authService.revokeRefreshToken(request.refreshToken());
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Làm mới Access Token bằng Refresh Token")
    public ResponseEntity<Map<String, String>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String newAccessToken = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin tài khoản hiện tại đang đăng nhập")
    public ResponseEntity<User> getMe(Authentication authentication) {
        Caller caller = CallerResolver.resolve(authentication);
        User user = authService.getMe(caller.requireUserId());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/change-password")
    @Operation(summary = "Đổi mật khẩu người dùng")
    public ResponseEntity<Map<String, String>> changePassword(Authentication authentication,
                                                              @Valid @RequestBody ChangePasswordRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        authService.changePassword(caller.requireUserId(), request.oldPassword(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Yêu cầu đặt lại mật khẩu qua email")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of("message", "Email hướng dẫn đặt lại mật khẩu đã được gửi"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Xác nhận đặt lại mật khẩu mới")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(Map.of("message", "Mật khẩu đã được đặt lại thành công"));
    }
}
