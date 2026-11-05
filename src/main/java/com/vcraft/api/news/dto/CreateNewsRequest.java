package com.vcraft.api.news.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

public record CreateNewsRequest(
        @NotBlank(message = "Tiêu đề tin tức không được để trống")
        String title,

        @NotBlank(message = "Nội dung bài viết không được để trống")
        String content,

        @NotBlank(message = "Loại thông báo không được để trống (price | schedule | policy)")
        String type,

        Instant publishedAt,
        List<String> images
) {}
