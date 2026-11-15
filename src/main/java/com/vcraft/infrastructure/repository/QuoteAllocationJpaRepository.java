package com.vcraft.infrastructure.repository;
import com.vcraft.infrastructure.entity.QuoteAllocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface QuoteAllocationJpaRepository extends JpaRepository<QuoteAllocationEntity, UUID> {
    List<QuoteAllocationEntity> findByParentId(UUID parentId);
}
