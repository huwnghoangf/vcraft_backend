package com.vcraft.domain.model;

import com.vcraft.domain.exception.ForbiddenException;
import com.vcraft.domain.exception.UnauthorizedException;
import java.util.UUID;

public record Caller(UUID userId, String email, Role role) {

    public static Caller anonymous() {
        return new Caller(null, null, null);
    }

    public static Caller authenticated(UUID userId, String email, Role role) {
        return new Caller(userId, email, role);
    }

    public boolean isAuthenticated() {
        return userId != null;
    }

    public boolean isBuyer() {
        return role == Role.BUYER;
    }

    public boolean isArtisan() {
        return role == Role.ARTISAN;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public UUID requireUserId() {
        if (!isAuthenticated()) {
            throw new UnauthorizedException("Vui lòng đăng nhập để thực hiện thao tác này");
        }
        return userId;
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new ForbiddenException("Chỉ Quản trị viên BQL mới có quyền thực hiện");
        }
    }

    public void requireArtisan() {
        if (!isArtisan() && !isAdmin()) {
            throw new ForbiddenException("Chỉ Nghệ nhân xưởng gốm mới có quyền thực hiện");
        }
    }
}
