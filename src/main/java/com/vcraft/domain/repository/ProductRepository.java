package com.vcraft.domain.repository;

import com.vcraft.domain.model.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Optional<Product> findById(UUID id);
    Product save(Product product);
    void deleteById(UUID id);
    List<Product> findProducts(UUID artisanId, UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice, int page, int limit, String sort);
    long countProducts(UUID artisanId, UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice);
    List<Product> findByArtisanId(UUID artisanId);
    List<Product> findByBatchId(UUID batchId);
}
