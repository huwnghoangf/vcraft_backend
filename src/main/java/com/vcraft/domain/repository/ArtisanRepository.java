package com.vcraft.domain.repository;

import com.vcraft.domain.model.ArtisanProfile;
import com.vcraft.domain.model.ArtisanStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtisanRepository {
    Optional<ArtisanProfile> findById(UUID id);
    Optional<ArtisanProfile> findByUserId(UUID userId);
    ArtisanProfile save(ArtisanProfile profile);
    List<ArtisanProfile> findAllApproved(int page, int limit, String search, String district);
    long countApproved(String search, String district);
    List<ArtisanProfile> findAllByStatus(ArtisanStatus status, int page, int limit);
    long countByStatus(ArtisanStatus status);
    List<ArtisanProfile> findAllForMap(boolean openOnly);
}
