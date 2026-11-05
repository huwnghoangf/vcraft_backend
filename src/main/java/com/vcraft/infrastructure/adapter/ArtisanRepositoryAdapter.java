package com.vcraft.infrastructure.adapter;

import com.vcraft.domain.model.ArtisanProfile;
import com.vcraft.domain.model.ArtisanStatus;
import com.vcraft.domain.repository.ArtisanRepository;
import com.vcraft.infrastructure.entity.ArtisanProfileEntity;
import com.vcraft.infrastructure.repository.ArtisanJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class ArtisanRepositoryAdapter implements ArtisanRepository {

    private final ArtisanJpaRepository jpaRepository;

    public ArtisanRepositoryAdapter(ArtisanJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ArtisanProfile> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<ArtisanProfile> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public ArtisanProfile save(ArtisanProfile profile) {
        ArtisanProfileEntity entity = toEntity(profile);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<ArtisanProfile> findAllApproved(int page, int limit, String search, String district) {
        var pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit), Sort.by(Sort.Direction.DESC, "createdAt"));
        return jpaRepository.searchApproved(search, district, pageable).getContent().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countApproved(String search, String district) {
        return jpaRepository.searchApproved(search, district, PageRequest.of(0, 1)).getTotalElements();
    }

    @Override
    public List<ArtisanProfile> findAllByStatus(ArtisanStatus status, int page, int limit) {
        var pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit), Sort.by(Sort.Direction.DESC, "createdAt"));
        return jpaRepository.findByStatus(status.name(), pageable).getContent().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByStatus(ArtisanStatus status) {
        return jpaRepository.countByStatus(status.name());
    }

    @Override
    public List<ArtisanProfile> findAllForMap(boolean openOnly) {
        return jpaRepository.findForMap(openOnly).stream()
                .map(this::toDomain)
                .toList();
    }

    private ArtisanProfile toDomain(ArtisanProfileEntity entity) {
        return new ArtisanProfile(
                entity.getId(),
                entity.getUserId(),
                entity.getWorkshopName(),
                entity.getOwnerName(),
                entity.getPhone(),
                entity.getAddress(),
                entity.getDistrict(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.isOpenStatus(),
                entity.getDescription(),
                entity.getFamilyHistory(),
                entity.getCertificates(),
                entity.getImages(),
                ArtisanStatus.valueOf(entity.getStatus()),
                entity.getRejectReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ArtisanProfileEntity toEntity(ArtisanProfile domain) {
        return new ArtisanProfileEntity(
                domain.id(),
                domain.userId(),
                domain.workshopName(),
                domain.ownerName(),
                domain.phone(),
                domain.address(),
                domain.district(),
                domain.latitude(),
                domain.longitude(),
                domain.openStatus(),
                domain.description(),
                domain.familyHistory(),
                domain.certificates(),
                domain.images(),
                domain.status().name(),
                domain.rejectReason(),
                domain.createdAt(),
                domain.updatedAt()
        );
    }
}
