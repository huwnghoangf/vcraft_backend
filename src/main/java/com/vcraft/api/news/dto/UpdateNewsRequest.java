package com.vcraft.api.news.dto;

public record UpdateNewsRequest(
        String title,
        String content,
        String type
) {}
