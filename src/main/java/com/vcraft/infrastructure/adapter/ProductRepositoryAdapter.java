package com.vcraft.infrastructure.adapter;

import com.vcraft.domain.model.Product;
import com.vcraft.domain.repository.ProductRepository;
import com.vcraft.infrastructure.entity.ProductEntity;
import com.vcraft.infrastructure.repository.ProductJpaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpaRepository;

    public ProductRepositoryAdapter(ProductJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Product save(Product product) {
        ProductEntity entity = toEntity(product);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<Product> findProducts(UUID artisanId, UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                      int page, int limit, String sort) {
        Sort sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
        if ("price_asc".equalsIgnoreCase(sort)) {
            sortObj = Sort.by(Sort.Direction.ASC, "price");
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            sortObj = Sort.by(Sort.Direction.DESC, "price");
        }

        var pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit), sortObj);
        return jpaRepository.searchProducts(artisanId, categoryId, minPrice, maxPrice, pageable)
                .getContent().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countProducts(UUID artisanId, UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        return jpaRepository.searchProducts(artisanId, categoryId, minPrice, maxPrice, PageRequest.of(0, 1))
                .getTotalElements();
    }

    @Override
    public List<Product> findByArtisanId(UUID artisanId) {
        return jpaRepository.findByArtisanId(artisanId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Product> findByBatchId(UUID batchId) {
        return jpaRepository.findByBatchId(batchId).stream().map(this::toDomain).toList();
    }

    private Product toDomain(ProductEntity entity) {
        return new Product(
                entity.getId(),
                entity.getArtisanId(),
                entity.getCategoryId(),
                entity.getBatchId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getStock(),
                entity.getImages(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ProductEntity toEntity(Product domain) {
        return new ProductEntity(
                domain.id(),
                domain.artisanId(),
                domain.categoryId(),
                domain.batchId(),
                domain.name(),
                domain.description(),
                domain.price(),
                domain.stock(),
                domain.images(),
                domain.createdAt(),
                domain.updatedAt()
        );
    }
}
