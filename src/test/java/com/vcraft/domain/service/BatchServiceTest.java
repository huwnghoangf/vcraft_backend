package com.vcraft.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vcraft.domain.exception.ForbiddenException;
import com.vcraft.domain.exception.NotFoundException;
import com.vcraft.domain.model.ArtisanProfile;
import com.vcraft.domain.model.ArtisanStatus;
import com.vcraft.domain.model.Batch;
import com.vcraft.domain.model.BatchStatus;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.Role;
import com.vcraft.domain.repository.ArtisanRepository;
import com.vcraft.domain.repository.BatchRepository;
import com.vcraft.domain.repository.ProductionLogRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BatchServiceTest {

    private static final UUID ARTISAN_USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final UUID ARTISAN_ID = UUID.randomUUID();
    private static final UUID OTHER_ARTISAN_ID = UUID.randomUUID();
    private static final UUID BATCH_ID = UUID.randomUUID();

    private BatchRepository batchRepository;
    private ProductionLogRepository logRepository;
    private ArtisanRepository artisanRepository;
    private QrCodeGenerator qrCodeGenerator;
    private BatchService batchService;

    private Batch batch;
    private ArtisanProfile artisanProfile;

    @BeforeEach
    void setUp() {
        batchRepository = mock(BatchRepository.class);
        logRepository = mock(ProductionLogRepository.class);
        artisanRepository = mock(ArtisanRepository.class);
        qrCodeGenerator = mock(QrCodeGenerator.class);

        batchService = new BatchService(
                batchRepository,
                logRepository,
                artisanRepository,
                qrCodeGenerator,
                "http://localhost:3000"
        );

        artisanProfile = new ArtisanProfile(
                ARTISAN_ID,
                ARTISAN_USER_ID,
                "Xưởng Gốm Hùng Thịnh",
                "Lê Văn Hùng",
                "0987654321",
                "Bát Tràng",
                "Gia Lâm, Hà Nội",
                20.97,
                105.91,
                true,
                "Xưởng gốm",
                null,
                List.of(),
                List.of(),
                ArtisanStatus.APPROVED,
                null,
                Instant.now(),
                Instant.now()
        );

        batch = new Batch(
                BATCH_ID,
                ARTISAN_ID,
                "Lô Bình Hút Lộc Men Rạn",
                "Lô sản xuất đặc biệt tháng 10",
                100,
                BatchStatus.IN_PROGRESS,
                "http://localhost:8080/uploads/qr/qr_1.png",
                "token123",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Lấy chi tiết lô sản xuất không tồn tại phải ném NotFoundException")
    void getNonExistentBatchThrowsNotFound() {
        UUID nonExistent = UUID.randomUUID();
        when(batchRepository.findById(nonExistent)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchService.getBatchDetail(nonExistent))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Không tìm thấy lô sản xuất");
    }

    @Test
    @DisplayName("Nghệ nhân trạng thái PENDING khi tạo lô sản xuất phải ném ForbiddenException")
    void createBatchWithPendingArtisanThrowsForbidden() {
        ArtisanProfile pendingProfile = new ArtisanProfile(
                ARTISAN_ID, ARTISAN_USER_ID, "Xưởng Chờ Duyệt", "Lê Văn Hùng", "0987654321",
                "Bát Tràng", "Gia Lâm, Hà Nội", 20.97, 105.91, true, "Xưởng gốm", null,
                List.of(), List.of(), ArtisanStatus.PENDING, null, Instant.now(), Instant.now()
        );
        when(artisanRepository.findByUserId(ARTISAN_USER_ID)).thenReturn(Optional.of(pendingProfile));
        Caller caller = Caller.authenticated(ARTISAN_USER_ID, "artisan@vcraft.vn", Role.ARTISAN);

        assertThatThrownBy(() -> batchService.createBatch(caller, "Lô gốm", "Mô tả", 50))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("đang chờ duyệt hoặc đã bị khóa");

        verify(batchRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo lô sản xuất khi chưa có hồ sơ nghệ nhân phải ném ForbiddenException")
    void createBatchWithoutArtisanProfileThrowsForbidden() {
        when(artisanRepository.findByUserId(ARTISAN_USER_ID)).thenReturn(Optional.empty());
        Caller caller = Caller.authenticated(ARTISAN_USER_ID, "artisan@vcraft.vn", Role.ARTISAN);

        assertThatThrownBy(() -> batchService.createBatch(caller, "Lô gốm", "Mô tả", 50))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Chỉ nghệ nhân mới được tạo lô sản xuất");

        verify(batchRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sửa lô sản xuất của nghệ nhân khác phải ném ForbiddenException")
    void updateBatchOfAnotherArtisanThrowsForbidden() {
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));

        // Người dùng khác là một nghệ nhân khác
        ArtisanProfile otherProfile = new ArtisanProfile(
                OTHER_ARTISAN_ID, OTHER_USER_ID, "Xưởng Khác", "Trần C", "0900000000",
                "Bát Tràng", "Hà Nội", 20.0, 105.0, true, "Xưởng", null, List.of(), List.of(),
                ArtisanStatus.APPROVED, null, Instant.now(), Instant.now()
        );
        when(artisanRepository.findByUserId(OTHER_USER_ID)).thenReturn(Optional.of(otherProfile));

        Caller otherCaller = Caller.authenticated(OTHER_USER_ID, "other@vcraft.vn", Role.ARTISAN);

        assertThatThrownBy(() -> batchService.updateBatch(otherCaller, BATCH_ID, "Tên mới", "Mô tả mới", "COMPLETED"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Lô sản xuất không thuộc xưởng của bạn");

        verify(batchRepository, never()).save(any());
    }
}
