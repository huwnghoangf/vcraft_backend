package com.vcraft.api.batch.dto;

import java.util.List;

public record UpdateLogRequest(
        String note,
        List<String> images
) {}
