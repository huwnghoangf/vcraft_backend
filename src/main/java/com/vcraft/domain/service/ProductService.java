package com.vcraft.domain.service;

import com.vcraft.domain.exception.ForbiddenException;
import com.vcraft.domain.exception.NotFoundException;
import com.vcraft.domain.model.ArtisanProfile;
import com.vcraft.domain.model.ArtisanStatus;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.Product;
import com.vcraft.domain.repository.ArtisanRepository;
import com.vcraft.domain.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductService {

    private final ProductRepository productRepository;
    private final ArtisanRepository artisanRepository;

    public ProductService(ProductRepository productRepository, ArtisanRepository artisanRepository) {
        this.productRepository = productRepository;
        this.artisanRepository = artisanRepository;
    }

    public List<Product> getProducts(UUID artisanId, UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                    int page, int limit, String sort) {
        return productRepository.findProducts(artisanId, categoryId, minPrice, maxPrice, page, limit, sort);
    }

    public long countProducts(UUID artisanId, UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        return productRepository.countProducts(artisanId, categoryId, minPrice, maxPrice);
    }

    public Product getProductDetail(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm gốm"));
    }

    public Product createProduct(Caller caller, String name, String description, BigDecimal price,
                                 int stock, UUID categoryId, List<String> images, UUID batchId) {
        UUID userId = caller.requireUserId();
        ArtisanProfile artisan = artisanRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Chỉ tài khoản nghệ nhân đã có xưởng mới được đăng sản phẩm"));

        if (artisan.status() != ArtisanStatus.APPROVED) {
            throw new ForbiddenException("Hồ sơ xưởng gốm của bạn đang chờ BQL duyệt hoặc đã bị từ chối. Chỉ xưởng đã được duyệt mới có thể đăng sản phẩm.");
        }

        Product product = new Product(
                UUID.randomUUID(),
                artisan.id(),
                categoryId,
                batchId,
                name,
                description,
                price,
                stock,
                images != null ? images : List.of(),
                Instant.now(),
                Instant.now()
        );
        return productRepository.save(product);
    }

    public Product updateProduct(Caller caller, UUID productId, String name, String description,
                                 BigDecimal price, int stock, List<String> images) {
        Product current = getProductDetail(productId);
        checkArtisanOwnership(caller, current.artisanId());

        Product updated = new Product(
                current.id(),
                current.artisanId(),
                current.categoryId(),
                current.batchId(),
                name != null ? name : current.name(),
                description != null ? description : current.description(),
                price != null ? price : current.price(),
                stock,
                images != null ? images : current.images(),
                current.createdAt(),
                Instant.now()
        );
        return productRepository.save(updated);
    }

    public void deleteProduct(Caller caller, UUID productId) {
        Product current = getProductDetail(productId);
        checkArtisanOwnership(caller, current.artisanId());
        productRepository.deleteById(productId);
    }

    public void addProductImages(Caller caller, UUID productId, List<String> newImages) {
        Product current = getProductDetail(productId);
        checkArtisanOwnership(caller, current.artisanId());

        List<String> images = new ArrayList<>(current.images() != null ? current.images() : List.of());
        images.addAll(newImages);

        Product updated = new Product(
                current.id(),
                current.artisanId(),
                current.categoryId(),
                current.batchId(),
                current.name(),
                current.description(),
                current.price(),
                current.stock(),
                images,
                current.createdAt(),
                Instant.now()
        );
        productRepository.save(updated);
    }

    private void checkArtisanOwnership(Caller caller, UUID artisanId) {
        if (caller.isAdmin()) return;
        UUID userId = caller.requireUserId();
        ArtisanProfile artisan = artisanRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền thao tác trên sản phẩm này"));
        if (artisan.status() != ArtisanStatus.APPROVED) {
            throw new ForbiddenException("Hồ sơ xưởng gốm của bạn chưa được duyệt hoặc đã bị khóa");
        }
        if (!artisan.id().equals(artisanId)) {
            throw new ForbiddenException("Sản phẩm không thuộc xưởng của bạn");
        }
    }
}
