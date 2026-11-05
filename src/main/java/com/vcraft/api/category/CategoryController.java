package com.vcraft.api.category;

import com.vcraft.api.CallerResolver;
import com.vcraft.api.category.dto.CategoryRequest;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.Category;
import com.vcraft.domain.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "3. Categories", description = "Các API danh mục gốm (Ấm chén, Bình hoa, Bát đĩa...)")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Danh sách danh mục sản phẩm gốm Bát Tràng")
    public ResponseEntity<List<Category>> getCategories() {
        return ResponseEntity.ok(categoryService.getCategories());
    }

    @PostMapping
    @Operation(summary = "Thêm danh mục mới (Chỉ Admin)")
    public ResponseEntity<Category> createCategory(Authentication authentication,
                                                   @Valid @RequestBody CategoryRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        Category category = categoryService.createCategory(caller, request.name(), request.description(), request.icon());
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật danh mục (Chỉ Admin)")
    public ResponseEntity<Category> updateCategory(Authentication authentication,
                                                   @PathVariable UUID id,
                                                   @Valid @RequestBody CategoryRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        Category category = categoryService.updateCategory(caller, id, request.name(), request.description(), request.icon());
        return ResponseEntity.ok(category);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa danh mục (Chỉ Admin)")
    public ResponseEntity<Map<String, String>> deleteCategory(Authentication authentication,
                                                              @PathVariable UUID id) {
        Caller caller = CallerResolver.resolve(authentication);
        categoryService.deleteCategory(caller, id);
        return ResponseEntity.ok(Map.of("message", "Deleted category successfully"));
    }
}
