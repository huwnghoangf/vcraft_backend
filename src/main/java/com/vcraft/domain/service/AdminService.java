package com.vcraft.domain.service;

import com.vcraft.domain.exception.NotFoundException;
import com.vcraft.domain.model.ArtisanProfile;
import com.vcraft.domain.model.ArtisanStatus;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.QuoteRequest;
import com.vcraft.domain.model.QuoteStatus;
import com.vcraft.domain.model.User;
import com.vcraft.domain.model.UserStatus;
import com.vcraft.domain.repository.ArtisanRepository;
import com.vcraft.domain.repository.ProductRepository;
import com.vcraft.domain.repository.QuoteRepository;
import com.vcraft.domain.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminService {

    private final ArtisanRepository artisanRepository;
    private final UserRepository userRepository;
    private final QuoteRepository quoteRepository;
    private final ProductRepository productRepository;

    public AdminService(ArtisanRepository artisanRepository,
                        UserRepository userRepository,
                        QuoteRepository quoteRepository,
                        ProductRepository productRepository) {
        this.artisanRepository = artisanRepository;
        this.userRepository = userRepository;
        this.quoteRepository = quoteRepository;
        this.productRepository = productRepository;
    }

    public List<ArtisanProfile> getPendingArtisans(Caller caller, int page, int limit) {
        caller.requireAdmin();
        return artisanRepository.findAllByStatus(ArtisanStatus.PENDING, page, limit);
    }

    public long countPendingArtisans(Caller caller) {
        caller.requireAdmin();
        return artisanRepository.countByStatus(ArtisanStatus.PENDING);
    }

    public ArtisanProfile approveArtisan(Caller caller, UUID artisanId) {
        caller.requireAdmin();
        ArtisanProfile artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hồ sơ nghệ nhân"));

        ArtisanProfile approved = new ArtisanProfile(
                artisan.id(), artisan.userId(), artisan.workshopName(), artisan.ownerName(),
                artisan.phone(), artisan.address(), artisan.district(), artisan.latitude(),
                artisan.longitude(), artisan.openStatus(), artisan.description(),
                artisan.familyHistory(), artisan.certificates(), artisan.images(),
                ArtisanStatus.APPROVED, null, artisan.createdAt(), Instant.now()
        );
        return artisanRepository.save(approved);
    }

    public ArtisanProfile rejectArtisan(Caller caller, UUID artisanId, String reason) {
        caller.requireAdmin();
        ArtisanProfile artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hồ sơ nghệ nhân"));

        ArtisanProfile rejected = new ArtisanProfile(
                artisan.id(), artisan.userId(), artisan.workshopName(), artisan.ownerName(),
                artisan.phone(), artisan.address(), artisan.district(), artisan.latitude(),
                artisan.longitude(), artisan.openStatus(), artisan.description(),
                artisan.familyHistory(), artisan.certificates(), artisan.images(),
                ArtisanStatus.REJECTED, reason, artisan.createdAt(), Instant.now()
        );
        return artisanRepository.save(rejected);
    }

    public ArtisanProfile blockArtisan(Caller caller, UUID artisanId, String reason) {
        caller.requireAdmin();
        ArtisanProfile artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hồ sơ nghệ nhân"));

        ArtisanProfile blocked = new ArtisanProfile(
                artisan.id(), artisan.userId(), artisan.workshopName(), artisan.ownerName(),
                artisan.phone(), artisan.address(), artisan.district(), artisan.latitude(),
                artisan.longitude(), false, artisan.description(),
                artisan.familyHistory(), artisan.certificates(), artisan.images(),
                ArtisanStatus.BLOCKED, reason, artisan.createdAt(), Instant.now()
        );
        ArtisanProfile saved = artisanRepository.save(blocked);

        // Khóa luôn tài khoản user tương ứng
        userRepository.findById(artisan.userId()).ifPresent(u -> {
            User blockedUser = new User(
                    u.id(), u.email(), u.passwordHash(), u.fullName(), u.phone(),
                    u.role(), UserStatus.BLOCKED, u.createdAt(), Instant.now()
            );
            userRepository.save(blockedUser);
        });

        return saved;
    }

    public List<User> getUsers(Caller caller, String role, int page, int limit, String search) {
        caller.requireAdmin();
        return userRepository.findAll(role, page, limit, search);
    }

    public long countUsers(Caller caller, String role, String search) {
        caller.requireAdmin();
        return userRepository.count(role, search);
    }

    public User blockUser(Caller caller, UUID userId, String reason) {
        caller.requireAdmin();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));

        User blocked = new User(
                user.id(), user.email(), user.passwordHash(), user.fullName(), user.phone(),
                user.role(), UserStatus.BLOCKED, user.createdAt(), Instant.now()
        );
        return userRepository.save(blocked);
    }

    public List<QuoteRequest> getAllQuotes(Caller caller, String status, int page, int limit) {
        caller.requireAdmin();
        QuoteStatus quoteStatus = status != null && !status.isBlank() ? QuoteStatus.valueOf(status.toUpperCase()) : null;
        return quoteRepository.findAll(quoteStatus, page, limit);
    }

    public long countAllQuotes(Caller caller, String status) {
        caller.requireAdmin();
        QuoteStatus quoteStatus = status != null && !status.isBlank() ? QuoteStatus.valueOf(status.toUpperCase()) : null;
        return quoteRepository.countAll(quoteStatus);
    }

    public Map<String, Object> getDashboardStats(Caller caller) {
        caller.requireAdmin();
        long totalArtisans = artisanRepository.countApproved(null, null);
        long pendingArtisans = artisanRepository.countByStatus(ArtisanStatus.PENDING);
        long totalProducts = productRepository.countProducts(null, null, null, null);
        long totalQuotes = quoteRepository.countAll(null);
        long totalBuyers = userRepository.count("BUYER", null);

        return Map.of(
                "totalArtisans", totalArtisans,
                "pendingArtisans", pendingArtisans,
                "totalProducts", totalProducts,
                "totalQuotes", totalQuotes,
                "totalBuyers", totalBuyers
        );
    }
}
