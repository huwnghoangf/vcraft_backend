package com.vcraft.api.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vcraft.config.SecurityConfig;
import com.vcraft.domain.exception.ConflictException;
import com.vcraft.domain.exception.UnauthorizedException;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.Role;
import com.vcraft.domain.model.User;
import com.vcraft.domain.model.UserStatus;
import com.vcraft.domain.service.AuthService;
import com.vcraft.infrastructure.security.JwtTokenProvider;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final User SAMPLE_USER = new User(
            USER_ID,
            "buyer@vcraft.vn",
            "hashed",
            "Nguyễn Văn A",
            "0912345678",
            Role.BUYER,
            UserStatus.ACTIVE,
            Instant.now(),
            Instant.now()
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    // ====== Đăng nhập - Error Tests ======

    @Test
    @DisplayName("Đăng nhập với mật khẩu sai trả về HTTP 401 Unauthorized")
    void loginWithWrongPasswordReturns401() throws Exception {
        when(authService.login("buyer@vcraft.vn", "wrongpass"))
                .thenThrow(new UnauthorizedException("Email hoặc mật khẩu không chính xác"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "buyer@vcraft.vn",
                                  "password": "wrongpass"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Email hoặc mật khẩu không chính xác"));
    }

    @Test
    @DisplayName("Đăng nhập thiếu email hoặc mật khẩu trả về HTTP 400 Bad Request")
    void loginWithMissingFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ====== Đăng ký - Error Tests ======

    @Test
    @DisplayName("Đăng ký Buyer với email đã trùng trả về HTTP 409 Conflict")
    void registerBuyerWithDuplicateEmailReturns409() throws Exception {
        when(authService.registerBuyer(any(), eq("existing@vcraft.vn"), any(), any()))
                .thenThrow(new ConflictException("Email đã được đăng ký trong hệ thống"));

        mockMvc.perform(post("/api/v1/auth/register/buyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Khách Mới",
                                  "email": "existing@vcraft.vn",
                                  "password": "secretPassword123"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email đã được đăng ký trong hệ thống"));
    }

    @Test
    @DisplayName("Đăng ký Buyer với email sai định dạng trả về HTTP 400 Bad Request")
    void registerBuyerWithInvalidEmailReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/buyer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Khách Mới",
                                  "email": "not-an-email",
                                  "password": "secretPassword123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ====== Refresh Token - Error Tests ======

    @Test
    @DisplayName("Refresh token không hợp lệ hoặc đã hết hạn trả về HTTP 401 Unauthorized")
    void refreshTokenInvalidReturns401() throws Exception {
        when(authService.refreshToken("expired-or-invalid"))
                .thenThrow(new UnauthorizedException("Refresh token không hợp lệ hoặc đã hết hạn"));

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "expired-or-invalid"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Refresh token không hợp lệ hoặc đã hết hạn"));
    }

    // ====== me - Error Tests ======

    @Test
    @DisplayName("Gọi /me khi chưa xác thực (chưa gửi token) trả về HTTP 401 Unauthorized")
    void getMeWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // ====== Happy Path Test ======

    @Test
    @DisplayName("Đăng nhập thông tin hợp lệ trả về HTTP 200 kèm Access Token")
    void loginWithValidCredentialsReturns200() throws Exception {
        AuthService.AuthResult result = new AuthService.AuthResult(SAMPLE_USER, "mock-access-token", "mock-refresh-token");
        when(authService.login("buyer@vcraft.vn", "validpass")).thenReturn(result);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "buyer@vcraft.vn",
                                  "password": "validpass"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token"))
                .andExpect(jsonPath("$.user.email").value("buyer@vcraft.vn"))
                .andExpect(jsonPath("$.user.fullName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.user.role").value("BUYER"));
    }
}
