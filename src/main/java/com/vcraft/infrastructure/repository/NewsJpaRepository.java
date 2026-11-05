package com.vcraft.infrastructure.repository;

import com.vcraft.infrastructure.entity.NewsEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsJpaRepository extends JpaRepository<NewsEntity, UUID> {
    @Query("SELECT n FROM NewsEntity n WHERE (:type IS NULL OR n.type = :type) ORDER BY n.publishedAt DESC")
    Page<NewsEntity> findByType(@Param("type") String type, Pageable pageable);

    @Query("SELECT COUNT(n) FROM NewsEntity n WHERE (:type IS NULL OR n.type = :type)")
    long countByType(@Param("type") String type);
}
