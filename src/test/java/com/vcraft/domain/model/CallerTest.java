package com.vcraft.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vcraft.domain.exception.ForbiddenException;
import com.vcraft.domain.exception.UnauthorizedException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CallerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("Caller ẩn danh khi gọi requireUserId() phải ném UnauthorizedException")
    void anonymousCallerRequiresUserIdThrowsUnauthorized() {
        Caller caller = Caller.anonymous();

        assertThat(caller.isAuthenticated()).isFalse();
        assertThatThrownBy(caller::requireUserId)
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Vui lòng đăng nhập");
    }

    @Test
    @DisplayName("Caller ẩn danh khi gọi requireAdmin() phải ném ForbiddenException")
    void anonymousCallerRequiresAdminThrowsForbidden() {
        Caller caller = Caller.anonymous();

        assertThatThrownBy(caller::requireAdmin)
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Chỉ Quản trị viên");
    }

    @Test
    @DisplayName("Caller ẩn danh khi gọi requireArtisan() phải ném ForbiddenException")
    void anonymousCallerRequiresArtisanThrowsForbidden() {
        Caller caller = Caller.anonymous();

        assertThatThrownBy(caller::requireArtisan)
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Chỉ Nghệ nhân");
    }

    @Test
    @DisplayName("Caller Buyer khi gọi requireAdmin() phải ném ForbiddenException")
    void buyerRequiresAdminThrowsForbidden() {
        Caller buyer = Caller.authenticated(USER_ID, "buyer@vcraft.vn", Role.BUYER);

        assertThat(buyer.isAuthenticated()).isTrue();
        assertThat(buyer.requireUserId()).isEqualTo(USER_ID);

        assertThatThrownBy(buyer::requireAdmin)
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Chỉ Quản trị viên");
    }

    @Test
    @DisplayName("Caller Buyer khi gọi requireArtisan() phải ném ForbiddenException")
    void buyerRequiresArtisanThrowsForbidden() {
        Caller buyer = Caller.authenticated(USER_ID, "buyer@vcraft.vn", Role.BUYER);

        assertThatThrownBy(buyer::requireArtisan)
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Chỉ Nghệ nhân");
    }

    @Test
    @DisplayName("Caller Artisan khi gọi requireAdmin() phải ném ForbiddenException")
    void artisanRequiresAdminThrowsForbidden() {
        Caller artisan = Caller.authenticated(USER_ID, "artisan@vcraft.vn", Role.ARTISAN);

        assertThatThrownBy(artisan::requireAdmin)
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Chỉ Quản trị viên");
    }
}
