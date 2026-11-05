package com.vcraft.api.product;

import com.vcraft.api.CallerResolver;
import com.vcraft.api.dto.PageResponse;
import com.vcraft.api.product.dto.CreateProductRequest;
import com.vcraft.api.product.dto.UpdateProductRequest;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.Product;
import com.vcraft.domain.service.ImageStorageService;
import com.vcraft.domain.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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
@RequestMapping("/api/v1/products")
@Tag(name = "3. Products", description = "Các API quản lý và tìm kiếm sản phẩm gốm Bát Tràng")
public class ProductController {

    private final ProductService productService;
    private final ImageStorageService imageStorageService;

    public ProductController(ProductService productService, ImageStorageService imageStorageService) {
        this.productService = productService;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping
    @Operation(summary = "Danh sách sản phẩm gốm (hỗ trợ lọc theo xưởng, danh mục, giá và sắp xếp)")
    public ResponseEntity<PageResponse<Product>> getProducts(
            @RequestParam(required = false) UUID artisanId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String sort
    ) {
        List<Product> data = productService.getProducts(artisanId, categoryId, minPrice, maxPrice, page, limit, sort);
        long total = productService.countProducts(artisanId, categoryId, minPrice, maxPrice);
        return ResponseEntity.ok(PageResponse.of(data, page, limit, total));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết một sản phẩm gốm Bát Tràng")
    public ResponseEntity<Product> getProductDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductDetail(id));
    }

    @PostMapping
    @Operation(summary = "Nghệ nhân đăng sản phẩm gốm mới")
    public ResponseEntity<Product> createProduct(Authentication authentication,
                                                 @Valid @RequestBody CreateProductRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        Product product = productService.createProduct(
                caller, request.name(), request.description(), request.price(),
                request.stock(), request.categoryId(), request.images(), request.batchId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin sản phẩm gốm")
    public ResponseEntity<Product> updateProduct(Authentication authentication,
                                                 @PathVariable UUID id,
                                                 @RequestBody UpdateProductRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        Product updated = productService.updateProduct(
                caller, id, request.name(), request.description(),
                request.price(), request.stock() != null ? request.stock() : 0, request.images()
        );
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa sản phẩm gốm")
    public ResponseEntity<Map<String, String>> deleteProduct(Authentication authentication,
                                                             @PathVariable UUID id) {
        Caller caller = CallerResolver.resolve(authentication);
        productService.deleteProduct(caller, id);
        return ResponseEntity.ok(Map.of("message", "Deleted product successfully"));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload thêm ảnh cho sản phẩm gốm (tối ưu hóa kích cỡ)")
    public ResponseEntity<List<String>> uploadProductImages(Authentication authentication,
                                                           @PathVariable UUID id,
                                                           @RequestParam("files") List<MultipartFile> files) {
        Caller caller = CallerResolver.resolve(authentication);
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                String url = imageStorageService.storeImage(file.getInputStream(), file.getOriginalFilename(), "products");
                imageUrls.add(url);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi lưu ảnh: " + e.getMessage());
            }
        }
        productService.addProductImages(caller, id, imageUrls);
        return ResponseEntity.ok(imageUrls);
    }
}
