package com.vcraft.domain.service;

import com.vcraft.domain.exception.NotFoundException;
import com.vcraft.domain.model.ArtisanProfile;
import com.vcraft.domain.model.Batch;
import com.vcraft.domain.model.Product;
import com.vcraft.domain.model.ProductionLog;
import com.vcraft.domain.repository.ArtisanRepository;
import com.vcraft.domain.repository.BatchRepository;
import com.vcraft.domain.repository.ProductRepository;
import com.vcraft.domain.repository.ProductionLogRepository;
import java.util.List;
import java.util.UUID;

public class TraceService {

    public record TraceStory(
            Batch batch,
            ArtisanProfile artisan,
            List<ProductionLog> logs,
            List<Product> products
    ) {}

    private final BatchRepository batchRepository;
    private final ArtisanRepository artisanRepository;
    private final ProductionLogRepository logRepository;
    private final ProductRepository productRepository;

    public TraceService(BatchRepository batchRepository,
                        ArtisanRepository artisanRepository,
                        ProductionLogRepository logRepository,
                        ProductRepository productRepository) {
        this.batchRepository = batchRepository;
        this.artisanRepository = artisanRepository;
        this.logRepository = logRepository;
        this.productRepository = productRepository;
    }

    public TraceStory getStoryByBatchId(UUID batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin lô gốm để truy xuất"));

        ArtisanProfile artisan = artisanRepository.findById(batch.artisanId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin nghệ nhân chế tác"));

        List<ProductionLog> logs = logRepository.findByBatchIdOrderByLoggedAtAsc(batchId);
        List<Product> products = productRepository.findByBatchId(batchId);

        return new TraceStory(batch, artisan, logs, products);
    }
}
