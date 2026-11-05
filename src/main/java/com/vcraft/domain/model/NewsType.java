package com.vcraft.domain.model;

public enum NewsType {
    PRICE,
    SCHEDULE,
    POLICY;

    public static NewsType fromString(String val) {
        if (val == null) return POLICY;
        try {
            return NewsType.valueOf(val.toUpperCase());
        } catch (IllegalArgumentException e) {
            return POLICY;
        }
    }
}
