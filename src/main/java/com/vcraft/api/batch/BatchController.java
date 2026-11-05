package com.vcraft.api.batch;

import com.vcraft.api.CallerResolver;
import com.vcraft.api.batch.dto.CreateBatchRequest;
import com.vcraft.api.batch.dto.CreateLogRequest;
import com.vcraft.api.batch.dto.UpdateBatchRequest;
import com.vcraft.api.batch.dto.UpdateLogRequest;
import com.vcraft.api.dto.PageResponse;
import com.vcraft.domain.model.Batch;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.ProductionLog;
import com.vcraft.domain.service.BatchService;
import com.vcraft.domain.service.ImageStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/batches")
@Tag(name = "4. Batch & Production Log", description = "Các API quản lý lô sản xuất, nhật ký quy trình gốm (Nhào đất -> Vuốt -> Nung -> Thành phẩm)")
public class BatchController {

    private final BatchService batchService;
    private final ImageStorageService imageStorageService;

    public BatchController(BatchService batchService, ImageStorageService imageStorageService) {
        this.batchService = batchService;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping
    @Operation(summary = "Danh sách lô sản xuất của xưởng đang đăng nhập")
    public ResponseEntity<PageResponse<Batch>> getMyBatches(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status
    ) {
        Caller caller = CallerResolver.resolve(authentication);
        List<Batch> data = batchService.getMyBatches(caller, status, page, limit);
        long total = batchService.countMyBatches(caller, status);
        return ResponseEntity.ok(PageResponse.of(data, page, limit, total));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết lô sản xuất (công khai qua QR code)")
    public ResponseEntity<Batch> getBatchDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(batchService.getBatchDetail(id));
    }

    @PostMapping
    @Operation(summary = "Tạo lô sản xuất mới (ví dụ: mẻ đất / mẻ nung)")
    public ResponseEntity<Batch> createBatch(Authentication authentication,
                                             @Valid @RequestBody CreateBatchRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        Batch batch = batchService.createBatch(caller, request.name(), request.description(), request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(batch);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin lô sản xuất")
    public ResponseEntity<Batch> updateBatch(Authentication authentication,
                                             @PathVariable UUID id,
                                             @RequestBody UpdateBatchRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        Batch updated = batchService.updateBatch(caller, id, request.name(), request.description(), request.status());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/regenerate-qr")
    @Operation(summary = "Sinh lại mã QR Code cho lô sản xuất")
    public ResponseEntity<Batch> regenerateQr(Authentication authentication,
                                              @PathVariable UUID id) {
        Caller caller = CallerResolver.resolve(authentication);
        return ResponseEntity.ok(batchService.regenerateQr(caller, id));
    }

    // Production Logs
    @PostMapping("/{id}/logs")
    @Operation(summary = "Thêm mốc nhật ký sản xuất mới cho lô")
    public ResponseEntity<ProductionLog> addLog(Authentication authentication,
                                                @PathVariable UUID id,
                                                @Valid @RequestBody CreateLogRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        ProductionLog log = batchService.addLog(caller, id, request.stage(), request.note(), request.images(), request.timestamp());
        return ResponseEntity.status(HttpStatus.CREATED).body(log);
    }

    @GetMapping("/{id}/logs")
    @Operation(summary = "Lấy toàn bộ nhật ký sản xuất của lô (công khai)")
    public ResponseEntity<List<ProductionLog>> getLogs(@PathVariable UUID id) {
        return ResponseEntity.ok(batchService.getLogs(id));
    }

    @PutMapping("/{id}/logs/{logId}")
    @Operation(summary = "Chỉnh sửa một mốc nhật ký sản xuất")
    public ResponseEntity<ProductionLog> updateLog(Authentication authentication,
                                                   @PathVariable UUID id,
                                                   @PathVariable UUID logId,
                                                   @RequestBody UpdateLogRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        return ResponseEntity.ok(batchService.updateLog(caller, id, logId, request.note(), request.images()));
    }

    @DeleteMapping("/{id}/logs/{logId}")
    @Operation(summary = "Xóa một mốc nhật ký sản xuất")
    public ResponseEntity<Map<String, String>> deleteLog(Authentication authentication,
                                                         @PathVariable UUID id,
                                                         @PathVariable UUID logId) {
        Caller caller = CallerResolver.resolve(authentication);
        batchService.deleteLog(caller, id, logId);
        return ResponseEntity.ok(Map.of("message", "Deleted log entry successfully"));
    }

    @PostMapping(value = "/{id}/logs/{logId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload ảnh minh chứng cho mốc nhật ký quy trình gốm")
    public ResponseEntity<List<String>> uploadLogImages(Authentication authentication,
                                                        @PathVariable UUID id,
                                                        @PathVariable UUID logId,
                                                        @RequestParam("files") List<MultipartFile> files) {
        Caller caller = CallerResolver.resolve(authentication);
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                String url = imageStorageService.storeImage(file.getInputStream(), file.getOriginalFilename(), "logs");
                imageUrls.add(url);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi lưu ảnh: " + e.getMessage());
            }
        }
        batchService.addLogImages(caller, id, logId, imageUrls);
        return ResponseEntity.ok(imageUrls);
    }
}
