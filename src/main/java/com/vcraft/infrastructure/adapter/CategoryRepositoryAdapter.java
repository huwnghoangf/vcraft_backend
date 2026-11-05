package com.vcraft.infrastructure.adapter;

import com.vcraft.domain.model.Category;
import com.vcraft.domain.repository.CategoryRepository;
import com.vcraft.infrastructure.entity.CategoryEntity;
import com.vcraft.infrastructure.repository.CategoryJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpaRepository jpaRepository;

    public CategoryRepositoryAdapter(CategoryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Category> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Category save(Category category) {
        CategoryEntity entity = new CategoryEntity(
                category.id(),
                category.name(),
                category.description(),
                category.icon(),
                category.createdAt()
        );
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private Category toDomain(CategoryEntity entity) {
        return new Category(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getIcon(),
                entity.getCreatedAt()
        );
    }
}
