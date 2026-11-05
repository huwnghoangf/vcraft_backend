package com.vcraft.domain.service;

import com.vcraft.domain.exception.NotFoundException;
import com.vcraft.domain.model.ArtisanProfile;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.repository.ArtisanRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ArtisanService {

    private final ArtisanRepository artisanRepository;

    public ArtisanService(ArtisanRepository artisanRepository) {
        this.artisanRepository = artisanRepository;
    }

    public List<ArtisanProfile> getApprovedArtisans(int page, int limit, String search, String district) {
        return artisanRepository.findAllApproved(page, limit, search, district);
    }

    public long countApproved(String search, String district) {
        return artisanRepository.countApproved(search, district);
    }

    public ArtisanProfile getArtisanDetail(UUID id) {
        return artisanRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin xưởng gốm"));
    }

    public ArtisanProfile getMyProfile(Caller caller) {
        UUID userId = caller.requireUserId();
        return artisanRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Hồ sơ xưởng gốm chưa được tạo hoặc không tồn tại"));
    }

    public ArtisanProfile updateMyProfile(Caller caller, String workshopName, String description,
                                          String address, Double lat, Double lng, Boolean openStatus,
                                          List<String> images, String familyHistory, List<String> certificates) {
        ArtisanProfile current = getMyProfile(caller);

        ArtisanProfile updated = new ArtisanProfile(
                current.id(),
                current.userId(),
                workshopName != null ? workshopName : current.workshopName(),
                current.ownerName(),
                current.phone(),
                address != null ? address : current.address(),
                current.district(),
                lat != null ? lat : current.latitude(),
                lng != null ? lng : current.longitude(),
                openStatus != null ? openStatus : current.openStatus(),
                description != null ? description : current.description(),
                familyHistory != null ? familyHistory : current.familyHistory(),
                certificates != null ? certificates : current.certificates(),
                images != null ? images : current.images(),
                current.status(),
                current.rejectReason(),
                current.createdAt(),
                Instant.now()
        );

        return artisanRepository.save(updated);
    }

    public ArtisanProfile setOpenStatus(Caller caller, boolean openStatus) {
        ArtisanProfile current = getMyProfile(caller);
        ArtisanProfile updated = new ArtisanProfile(
                current.id(),
                current.userId(),
                current.workshopName(),
                current.ownerName(),
                current.phone(),
                current.address(),
                current.district(),
                current.latitude(),
                current.longitude(),
                openStatus,
                current.description(),
                current.familyHistory(),
                current.certificates(),
                current.images(),
                current.status(),
                current.rejectReason(),
                current.createdAt(),
                Instant.now()
        );
        return artisanRepository.save(updated);
    }

    public ArtisanProfile updateLocation(Caller caller, Double lat, Double lng, String address) {
        ArtisanProfile current = getMyProfile(caller);
        ArtisanProfile updated = new ArtisanProfile(
                current.id(),
                current.userId(),
                current.workshopName(),
                current.ownerName(),
                current.phone(),
                address != null ? address : current.address(),
                current.district(),
                lat,
                lng,
                current.openStatus(),
                current.description(),
                current.familyHistory(),
                current.certificates(),
                current.images(),
                current.status(),
                current.rejectReason(),
                current.createdAt(),
                Instant.now()
        );
        return artisanRepository.save(updated);
    }

    public List<ArtisanProfile> getMapPins(boolean openOnly) {
        return artisanRepository.findAllForMap(openOnly);
    }

    public void addWorkshopImages(Caller caller, List<String> newImages) {
        ArtisanProfile current = getMyProfile(caller);
        List<String> images = new ArrayList<>(current.images() != null ? current.images() : List.of());
        images.addAll(newImages);

        ArtisanProfile updated = new ArtisanProfile(
                current.id(),
                current.userId(),
                current.workshopName(),
                current.ownerName(),
                current.phone(),
                current.address(),
                current.district(),
                current.latitude(),
                current.longitude(),
                current.openStatus(),
                current.description(),
                current.familyHistory(),
                current.certificates(),
                images,
                current.status(),
                current.rejectReason(),
                current.createdAt(),
                Instant.now()
        );
        artisanRepository.save(updated);
    }

    public void removeWorkshopImage(Caller caller, String imageId) {
        ArtisanProfile current = getMyProfile(caller);
        List<String> images = new ArrayList<>(current.images() != null ? current.images() : List.of());
        images.removeIf(img -> img.contains(imageId));

        ArtisanProfile updated = new ArtisanProfile(
                current.id(),
                current.userId(),
                current.workshopName(),
                current.ownerName(),
                current.phone(),
                current.address(),
                current.district(),
                current.latitude(),
                current.longitude(),
                current.openStatus(),
                current.description(),
                current.familyHistory(),
                current.certificates(),
                images,
                current.status(),
                current.rejectReason(),
                current.createdAt(),
                Instant.now()
        );
        artisanRepository.save(updated);
    }
}
