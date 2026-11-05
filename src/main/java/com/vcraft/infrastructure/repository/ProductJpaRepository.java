package com.vcraft.infrastructure.repository;

import com.vcraft.infrastructure.entity.ProductEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {
    List<ProductEntity> findByArtisanId(UUID artisanId);
    List<ProductEntity> findByBatchId(UUID batchId);

    @Query("SELECT p FROM ProductEntity p WHERE " +
           "(:artisanId IS NULL OR p.artisanId = :artisanId) AND " +
           "(:categoryId IS NULL OR p.categoryId = :categoryId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<ProductEntity> searchProducts(@Param("artisanId") UUID artisanId,
                                      @Param("categoryId") UUID categoryId,
                                      @Param("minPrice") BigDecimal minPrice,
                                      @Param("maxPrice") BigDecimal maxPrice,
                                      Pageable pageable);
}
