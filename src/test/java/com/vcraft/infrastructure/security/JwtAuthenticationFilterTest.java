package com.vcraft.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.Role;
import com.vcraft.domain.model.User;
import com.vcraft.domain.model.UserStatus;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private JwtTokenProvider tokenProvider;
    private JwtAuthenticationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        tokenProvider = mock(JwtTokenProvider.class);
        filter = new JwtAuthenticationFilter(tokenProvider);
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Request không có Header Authorization thì không xác thực và tiếp tục chuỗi filter")
    void requestWithoutAuthHeaderPassesThroughUnauthenticated() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Request với Header Authorization không bắt đầu bằng 'Bearer ' thì bị bỏ qua")
    void requestWithNonBearerHeaderPassesThroughUnauthenticated() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Request với token không hợp lệ (tampered/expired) thì không xác thực")
    void requestWithInvalidTokenPassesThroughUnauthenticated() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-or-expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(tokenProvider.parseClaims("invalid-or-expired-token")).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Request với token hợp lệ thì thiết lập Caller vào SecurityContext thành công")
    void requestWithValidTokenSetsAuthentication() throws ServletException, IOException {
        UUID userId = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(claims.get("email", String.class)).thenReturn("artisan@vcraft.vn");
        when(claims.get("role", String.class)).thenReturn("ARTISAN");

        when(tokenProvider.parseClaims("valid-token")).thenReturn(Optional.of(claims));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        Caller caller = (Caller) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(caller.userId()).isEqualTo(userId);
        assertThat(caller.email()).isEqualTo("artisan@vcraft.vn");
        assertThat(caller.role()).isEqualTo(Role.ARTISAN);
        verify(filterChain).doFilter(request, response);
    }
}
