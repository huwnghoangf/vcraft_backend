package com.vcraft.domain.service;
import com.vcraft.domain.exception.ForbiddenException;
import com.vcraft.domain.exception.NotFoundException;
import com.vcraft.domain.model.ArtisanProfile;
import com.vcraft.domain.model.ArtisanStatus;
import com.vcraft.domain.model.Batch;
import com.vcraft.domain.model.BatchStatus;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.ProductionLog;
import com.vcraft.domain.repository.ArtisanRepository;
import com.vcraft.domain.repository.BatchRepository;
import com.vcraft.domain.repository.ProductionLogRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
public class BatchService {
    private final BatchRepository batchRepository;
    private final ProductionLogRepository logRepository;
    private final ArtisanRepository artisanRepository;
    private final QrCodeGenerator qrCodeGenerator;
    private final String frontendUrl;
    public BatchService(BatchRepository batchRepository,
                        ProductionLogRepository logRepository,
                        ArtisanRepository artisanRepository,
                        QrCodeGenerator qrCodeGenerator,
                        String frontendUrl) {
        this.batchRepository = batchRepository;
        this.logRepository = logRepository;
        this.artisanRepository = artisanRepository;
        this.qrCodeGenerator = qrCodeGenerator;
        this.frontendUrl = frontendUrl != null ? frontendUrl : "http://localhost:3000";
    }
    public List<Batch> getMyBatches(Caller caller, String status, int page, int limit) {
        UUID userId = caller.requireUserId();
        ArtisanProfile artisan = artisanRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Chưa có hồ sơ xưởng gốm"));
        return batchRepository.findByArtisanId(artisan.id(), status, page, limit);
    }
    public long countMyBatches(Caller caller, String status) {
        UUID userId = caller.requireUserId();
        ArtisanProfile artisan = artisanRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Chưa có hồ sơ xưởng gốm"));
        return batchRepository.countByArtisanId(artisan.id(), status);
    }
    public Batch getBatchDetail(UUID id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lô sản xuất"));
    }
    public Batch createBatch(Caller caller, String name, String description, int quantity) {
        UUID userId = caller.requireUserId();
        ArtisanProfile artisan = artisanRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Chỉ nghệ nhân mới được tạo lô sản xuất"));
        if (artisan.status() != ArtisanStatus.APPROVED) {
            throw new ForbiddenException("Hồ sơ xưởng gốm của bạn đang chờ duyệt hoặc đã bị khóa. Chỉ nghệ nhân được duyệt mới có thể tạo lô sản xuất.");
        }
        if (name == null || name.isBlank() || quantity <= 0)
            throw new com.vcraft.domain.exception.InvalidInputException("Tên lô và số lượng không hợp lệ");
        UUID batchId = UUID.randomUUID();
        String qrToken = UUID.randomUUID().toString().replace("-", "");
        String traceUrl = frontendUrl + "/trace/" + batchId;
        String qrCodeUrl = qrCodeGenerator.generateQrCode(traceUrl, "qr_batch_" + batchId);
        Batch batch = new Batch(
                batchId,
                artisan.id(),
                name,
                description,
                quantity > 0 ? quantity : 1,
                BatchStatus.IN_PROGRESS,
                qrCodeUrl,
                qrToken,
                Instant.now(),
                Instant.now()
        );
        return batchRepository.save(batch);
    }
    public Batch updateBatch(Caller caller, UUID batchId, String name, String description, String status) {
        Batch current = getBatchDetail(batchId);
        checkArtisanOwnership(caller, current.artisanId());
        if (name != null && name.isBlank()) throw new com.vcraft.domain.exception.InvalidInputException("Tên lô không được trống");
        BatchStatus newStatus = current.status();
        if (status != null) {
            try {
                newStatus = BatchStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new com.vcraft.domain.exception.InvalidInputException("Trạng thái lô không hợp lệ");
            }
        }
        Batch updated = new Batch(
                current.id(),
                current.artisanId(),
                name != null ? name : current.name(),
                description != null ? description : current.description(),
                current.quantity(),
                newStatus,
                current.qrCodeUrl(),
                current.qrToken(),
                current.createdAt(),
                Instant.now()
        );
        return batchRepository.save(updated);
    }
    public Batch regenerateQr(Caller caller, UUID batchId) {
        Batch current = getBatchDetail(batchId);
        checkArtisanOwnership(caller, current.artisanId());
        String newQrToken = UUID.randomUUID().toString().replace("-", "");
        String traceUrl = frontendUrl + "/trace/" + batchId;
        String qrCodeUrl = qrCodeGenerator.generateQrCode(traceUrl, "qr_batch_" + batchId + "_" + System.currentTimeMillis());
        Batch updated = new Batch(
                current.id(),
                current.artisanId(),
                current.name(),
                current.description(),
                current.quantity(),
                current.status(),
                qrCodeUrl,
                newQrToken,
                current.createdAt(),
                Instant.now()
        );
        return batchRepository.save(updated);
    }
    // Production Logs
    public ProductionLog addLog(Caller caller, UUID batchId, String stage, String note, List<String> images, Instant timestamp) {
        Batch batch = getBatchDetail(batchId);
        checkArtisanOwnership(caller, batch.artisanId());
        ProductionLog log = new ProductionLog(
                UUID.randomUUID(),
                batchId,
                stage,
                note,
                images != null ? images : List.of(),
                timestamp != null ? timestamp : Instant.now(),
                Instant.now()
        );
        return logRepository.save(log);
    }
    public List<ProductionLog> getLogs(UUID batchId) {
        // Public endpoint qua QR code
        return logRepository.findByBatchIdOrderByLoggedAtAsc(batchId);
    }
    public ProductionLog updateLog(Caller caller, UUID batchId, UUID logId, String note, List<String> images) {
        Batch batch = getBatchDetail(batchId);
        checkArtisanOwnership(caller, batch.artisanId());
        ProductionLog current = logRepository.findById(logId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhật ký sản xuất"));
        checkLogBatch(current, batchId);
        ProductionLog updated = new ProductionLog(
                current.id(),
                current.batchId(),
                current.stage(),
                note != null ? note : current.note(),
                images != null ? images : current.images(),
                current.loggedAt(),
                current.createdAt()
        );
        return logRepository.save(updated);
    }
    public void deleteLog(Caller caller, UUID batchId, UUID logId) {
        Batch batch = getBatchDetail(batchId);
        checkArtisanOwnership(caller, batch.artisanId());
        ProductionLog current = logRepository.findById(logId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhật ký"));
        checkLogBatch(current, batchId);
        logRepository.deleteById(logId);
    }
    public void addLogImages(Caller caller, UUID batchId, UUID logId, List<String> newImages) {
        Batch batch = getBatchDetail(batchId);
        checkArtisanOwnership(caller, batch.artisanId());
        ProductionLog current = logRepository.findById(logId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mốc nhật ký"));
        checkLogBatch(current, batchId);
        List<String> images = new ArrayList<>(current.images() != null ? current.images() : List.of());
        images.addAll(newImages);
        checkLogBatch(current, batchId);
        ProductionLog updated = new ProductionLog(
                current.id(),
                current.batchId(),
                current.stage(),
                current.note(),
                images,
                current.loggedAt(),
                current.createdAt()
        );
        logRepository.save(updated);
    }
    public void authorizeLogImageUpload(Caller caller, UUID batchId, UUID logId) {
        checkArtisanOwnership(caller, getBatchDetail(batchId).artisanId());
        var log = logRepository.findById(logId).orElseThrow(() -> new NotFoundException("Không tìm thấy nhật ký"));
        checkLogBatch(log, batchId);
    }
    private void checkLogBatch(ProductionLog log, UUID batchId) {
        if (!log.batchId().equals(batchId)) throw new ForbiddenException("Nhật ký không thuộc lô này");
    }
    private void checkArtisanOwnership(Caller caller, UUID artisanId) {
        if (caller.isAdmin()) return;
        UUID userId = caller.requireUserId();
        ArtisanProfile artisan = artisanRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền thao tác trên lô sản xuất này"));
        if (artisan.status() != ArtisanStatus.APPROVED)
            throw new ForbiddenException("Xưởng chưa được duyệt hoặc đã bị khóa");
        if (!artisan.id().equals(artisanId)) {
            throw new ForbiddenException("Lô sản xuất không thuộc xưởng của bạn");
        }
    }
}
