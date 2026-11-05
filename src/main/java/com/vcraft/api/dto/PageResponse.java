package com.vcraft.api.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> data,
        int page,
        int limit,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> of(List<T> data, int page, int limit, long totalElements) {
        int totalPages = limit > 0 ? (int) Math.ceil((double) totalElements / limit) : 0;
        return new PageResponse<>(data, page, limit, totalElements, totalPages);
    }
}
