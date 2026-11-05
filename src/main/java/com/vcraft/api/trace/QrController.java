package com.vcraft.api.trace;

import com.vcraft.domain.model.Batch;
import com.vcraft.domain.service.BatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/qr")
@Tag(name = "5. QR Code", description = "Các API truy xuất và tải mã QR lô sản xuất gốm")
public class QrController {

    private final BatchService batchService;

    public QrController(BatchService batchService) {
        this.batchService = batchService;
    }

    @GetMapping("/{batchId}")
    @Operation(summary = "Lấy đường dẫn ảnh mã QR Code của lô sản xuất")
    public ResponseEntity<Map<String, String>> getQrCode(@PathVariable UUID batchId) {
        Batch batch = batchService.getBatchDetail(batchId);
        return ResponseEntity.ok(Map.of(
                "batchId", batch.id().toString(),
                "qrCodeUrl", batch.qrCodeUrl() != null ? batch.qrCodeUrl() : "",
                "downloadUrl", batch.qrCodeUrl() != null ? batch.qrCodeUrl() : ""
        ));
    }
}
