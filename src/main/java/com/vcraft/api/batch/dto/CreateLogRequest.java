package com.vcraft.api.batch.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

public record CreateLogRequest(
        @NotBlank(message = "Công đoạn không được để trống (Nhào đất | Vuốt gốm | Trang trí | Nung lò | Thành phẩm)")
        String stage,
        String note,
        List<String> images,
        Instant timestamp
) {}
