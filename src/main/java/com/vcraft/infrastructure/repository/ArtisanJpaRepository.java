package com.vcraft.infrastructure.repository;

import com.vcraft.infrastructure.entity.ArtisanProfileEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtisanJpaRepository extends JpaRepository<ArtisanProfileEntity, UUID> {
    Optional<ArtisanProfileEntity> findByUserId(UUID userId);

    @Query("SELECT a FROM ArtisanProfileEntity a WHERE a.status = 'APPROVED' AND " +
           "(:district IS NULL OR a.district = :district) AND " +
           "(:search IS NULL OR LOWER(a.workshopName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(a.ownerName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<ArtisanProfileEntity> searchApproved(@Param("search") String search, @Param("district") String district, Pageable pageable);

    Page<ArtisanProfileEntity> findByStatus(String status, Pageable pageable);
    long countByStatus(String status);

    @Query("SELECT a FROM ArtisanProfileEntity a WHERE a.status = 'APPROVED' AND a.latitude BETWEEN -90 AND 90 AND a.longitude BETWEEN -180 AND 180 AND (:openOnly = false OR a.openStatus = true)")
    List<ArtisanProfileEntity> findForMap(@Param("openOnly") boolean openOnly);
}
