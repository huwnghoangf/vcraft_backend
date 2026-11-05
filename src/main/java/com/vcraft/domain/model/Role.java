package com.vcraft.domain.model;

public enum Role {
    BUYER,
    ARTISAN,
    ADMIN;

    public static Role fromString(String role) {
        if (role == null) return BUYER;
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BUYER;
        }
    }
}
