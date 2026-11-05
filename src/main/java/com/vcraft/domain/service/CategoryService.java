package com.vcraft.domain.service;

import com.vcraft.domain.exception.NotFoundException;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.Category;
import com.vcraft.domain.repository.CategoryRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }

    public Category createCategory(Caller caller, String name, String description, String icon) {
        caller.requireAdmin();
        Category category = new Category(
                UUID.randomUUID(),
                name,
                description,
                icon,
                Instant.now()
        );
        return categoryRepository.save(category);
    }

    public Category updateCategory(Caller caller, UUID id, String name, String description, String icon) {
        caller.requireAdmin();
        Category current = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục"));

        Category updated = new Category(
                current.id(),
                name != null ? name : current.name(),
                description != null ? description : current.description(),
                icon != null ? icon : current.icon(),
                current.createdAt()
        );
        return categoryRepository.save(updated);
    }

    public void deleteCategory(Caller caller, UUID id) {
        caller.requireAdmin();
        categoryRepository.deleteById(id);
    }
}
