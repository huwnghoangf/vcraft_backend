package com.vcraft.api.trace;

import com.vcraft.domain.service.TraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trace")
@Tag(name = "5. Traceability", description = "Landing page truy xuất nguồn gốc (dành cho khách hàng quét mã QR trên sản phẩm gốm)")
public class TraceController {

    private final TraceService traceService;

    public TraceController(TraceService traceService) {
        this.traceService = traceService;
    }

    @GetMapping("/{batchId}")
    @Operation(summary = "Landing page truy xuất nguồn gốc: Câu chuyện lô gốm + Thông tin nghệ nhân + Nhật ký từng công đoạn vuốt nung")
    public ResponseEntity<TraceService.TraceStory> getTraceStory(@PathVariable UUID batchId) {
        return ResponseEntity.ok(traceService.getStoryByBatchId(batchId));
    }
}
