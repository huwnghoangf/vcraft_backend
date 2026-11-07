package com.vcraft.domain.service;



import com.vcraft.domain.exception.ConflictException;

import com.vcraft.domain.exception.ForbiddenException;

import com.vcraft.domain.exception.InvalidInputException;

import com.vcraft.domain.exception.NotFoundException;

import com.vcraft.domain.model.ArtisanProfile;
import com.vcraft.domain.model.ArtisanStatus;

import com.vcraft.domain.model.Bid;

import com.vcraft.domain.model.BidStatus;

import com.vcraft.domain.model.Caller;

import com.vcraft.domain.model.QuoteRequest;

import com.vcraft.domain.model.QuoteStatus;

import com.vcraft.domain.repository.ArtisanRepository;

import com.vcraft.domain.repository.BidRepository;

import com.vcraft.domain.repository.QuoteRepository;

import java.math.BigDecimal;

import java.time.Instant;

import java.util.List;

import java.util.UUID;



public class QuoteService {



    public record QuoteWithBids(QuoteRequest quote, List<Bid> bids) {}



    private final QuoteRepository quoteRepository;

    private final BidRepository bidRepository;

    private final ArtisanRepository artisanRepository;



    public QuoteService(QuoteRepository quoteRepository,

                        BidRepository bidRepository,

                        ArtisanRepository artisanRepository) {

        this.quoteRepository = quoteRepository;

        this.bidRepository = bidRepository;

        this.artisanRepository = artisanRepository;

    }



    public List<QuoteRequest> getMyQuotes(Caller caller, String status, int page, int limit) {

        UUID buyerId = caller.requireUserId();

        QuoteStatus quoteStatus = parseStatus(status);

        return quoteRepository.findByBuyerId(buyerId, quoteStatus, page, limit);

    }



    public long countMyQuotes(Caller caller, String status) {

        UUID buyerId = caller.requireUserId();

        QuoteStatus quoteStatus = parseStatus(status);

        return quoteRepository.countByBuyerId(buyerId, quoteStatus);

    }



    public QuoteRequest createQuote(Caller caller, String title, String description, int quantity,

                                    String unit, Instant deadline, UUID categoryId, List<UUID> targetArtisanIds) {

        UUID buyerId = caller.requireUserId();



        QuoteRequest quote = new QuoteRequest(

                UUID.randomUUID(),

                buyerId,

                categoryId,

                title,

                description,

                quantity > 0 ? quantity : 1,

                unit != null ? unit : "cái",

                deadline != null ? deadline : Instant.now().plusSeconds(86400 * 30),

                QuoteStatus.DRAFT,

                targetArtisanIds != null ? targetArtisanIds : List.of(),

                null,

                List.of(),

                null,

                0,

                Instant.now(),

                Instant.now()

        );

        return quoteRepository.save(quote);

    }



    public QuoteWithBids getQuoteDetail(Caller caller, UUID quoteId) {

        QuoteRequest quote = quoteRepository.findById(quoteId)

                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu báo giá"));



        if (!caller.isAdmin()) {

            UUID userId = caller.requireUserId();

            if (caller.isBuyer()) {

                if (!quote.buyerId().equals(userId)) {

                    throw new ForbiddenException("Bạn không có quyền xem yêu cầu báo giá của người khác");

                }

            } else if (caller.isArtisan()) {

                if (quote.status() == QuoteStatus.DRAFT) {

                    throw new ForbiddenException("Yêu cầu báo giá đang ở trạng thái bản nháp của khách hàng");

                }

                ArtisanProfile artisan = artisanRepository.findByUserId(userId)

                        .orElseThrow(() -> new ForbiddenException("Chưa có hồ sơ nghệ nhân"));

                if (quote.targetArtisanIds() != null && !quote.targetArtisanIds().isEmpty()

                        && !quote.targetArtisanIds().contains(artisan.id())) {

                    throw new ForbiddenException("Yêu cầu báo giá này không được gửi tới xưởng của bạn");

                }

            } else {

                throw new ForbiddenException("Bạn không có quyền xem yêu cầu báo giá này");

            }

        }



        List<Bid> bids = bidRepository.findByQuoteRequestId(quoteId);

        return new QuoteWithBids(quote, bids);

    }



    public QuoteRequest updateDraftQuote(Caller caller, UUID quoteId, String title,

                                         String description, Integer quantity, Instant deadline) {

        QuoteRequest current = getQuote(quoteId);

        checkBuyerOwnership(caller, current.buyerId());



        if (current.status() != QuoteStatus.DRAFT) {

            throw new ConflictException("Chỉ có thể chỉnh sửa yêu cầu báo giá khi còn ở trạng thái Bản nháp (Draft)");

        }



        QuoteRequest updated = new QuoteRequest(

                current.id(),

                current.buyerId(),

                current.categoryId(),

                title != null ? title : current.title(),

                description != null ? description : current.description(),

                quantity != null && quantity > 0 ? quantity : current.quantity(),

                current.unit(),

                deadline != null ? deadline : current.deadline(),

                current.status(),

                current.targetArtisanIds(),

                current.deliveryNote(),

                current.deliveryImages(),

                current.acceptedBidId(),

                current.version() + 1,

                current.createdAt(),

                Instant.now()

        );

        return quoteRepository.save(updated);

    }



    public QuoteRequest submitQuote(Caller caller, UUID quoteId) {

        QuoteRequest current = getQuote(quoteId);

        checkBuyerOwnership(caller, current.buyerId());



        if (!current.canBeSubmitted()) {

            throw new ConflictException("Yêu cầu báo giá đã được gửi hoặc không ở trạng thái Bản nháp");

        }



        QuoteRequest updated = new QuoteRequest(

                current.id(),

                current.buyerId(),

                current.categoryId(),

                current.title(),

                current.description(),

                current.quantity(),

                current.unit(),

                current.deadline(),

                QuoteStatus.SENT,

                current.targetArtisanIds(),

                current.deliveryNote(),

                current.deliveryImages(),

                current.acceptedBidId(),

                current.version() + 1,

                current.createdAt(),

                Instant.now()

        );

        return quoteRepository.save(updated);

    }



    public void deleteDraftQuote(Caller caller, UUID quoteId) {

        QuoteRequest current = getQuote(quoteId);

        checkBuyerOwnership(caller, current.buyerId());



        if (current.status() != QuoteStatus.DRAFT) {

            throw new ConflictException("Chỉ có thể hủy yêu cầu khi còn ở trạng thái Bản nháp");

        }

        quoteRepository.deleteById(quoteId);

    }



    public List<QuoteRequest> getIncomingQuotesForArtisan(Caller caller, String status, int page, int limit) {

        UUID userId = caller.requireUserId();

        ArtisanProfile artisan = artisanRepository.findByUserId(userId)

                .orElseThrow(() -> new ForbiddenException("Chưa có hồ sơ nghệ nhân"));



        QuoteStatus quoteStatus = parseStatus(status);

        return quoteRepository.findIncomingForArtisan(artisan.id(), quoteStatus, page, limit);

    }



    public Bid placeBid(Caller caller, UUID quoteId, BigDecimal pricePerUnit, BigDecimal totalPrice,

                        int estimatedDays, String note, String deliveryTerms) {

        QuoteRequest quote = getQuote(quoteId);

        if (!quote.canReceiveBids()) {

            throw new ConflictException("Yêu cầu báo giá hiện không trong thời gian tiếp nhận chào giá");

        }



        UUID userId = caller.requireUserId();

        ArtisanProfile artisan = artisanRepository.findByUserId(userId)

                .orElseThrow(() -> new ForbiddenException("Chỉ nghệ nhân mới được đặt giá thầu"));



        if (artisan.status() != ArtisanStatus.APPROVED) {

            throw new ForbiddenException("Hồ sơ xưởng gốm của bạn đang chờ duyệt hoặc đã bị khóa. Chỉ nghệ nhân được duyệt mới có thể tham gia chào giá.");

        }



        bidRepository.findByQuoteRequestIdAndArtisanId(quoteId, artisan.id())

                .ifPresent(existing -> {

                    throw new ConflictException("Bạn đã gửi chào giá cho yêu cầu này rồi. Vui lòng chỉnh sửa giá thầu hiện tại.");

                });



        Bid bid = new Bid(

                UUID.randomUUID(),

                quoteId,

                artisan.id(),

                pricePerUnit,

                totalPrice != null ? totalPrice : pricePerUnit.multiply(BigDecimal.valueOf(quote.quantity())),

                estimatedDays,

                note,

                deliveryTerms,

                BidStatus.PENDING,

                Instant.now(),

                Instant.now()

        );

        Bid savedBid = bidRepository.save(bid);



        if (quote.status() == QuoteStatus.SENT) {

            QuoteRequest updatedQuote = new QuoteRequest(

                    quote.id(), quote.buyerId(), quote.categoryId(), quote.title(), quote.description(),

                    quote.quantity(), quote.unit(), quote.deadline(), QuoteStatus.BIDDING,

                    quote.targetArtisanIds(), quote.deliveryNote(), quote.deliveryImages(),

                    quote.acceptedBidId(), quote.version() + 1, quote.createdAt(), Instant.now()

            );

            quoteRepository.save(updatedQuote);

        }



        return savedBid;

    }



    public Bid updateBid(Caller caller, UUID quoteId, UUID bidId, BigDecimal pricePerUnit,

                         Integer estimatedDays, String note) {

        Bid current = bidRepository.findById(bidId)

                .orElseThrow(() -> new NotFoundException("Không tìm thấy giá thầu"));



        checkArtisanOwnership(caller, current.artisanId());



        if (current.status() != BidStatus.PENDING) {

            throw new ConflictException("Không thể sửa giá thầu đã được chấp thuận hoặc từ chối");

        }



        QuoteRequest quote = getQuote(quoteId);

        BigDecimal newTotal = pricePerUnit != null ? pricePerUnit.multiply(BigDecimal.valueOf(quote.quantity())) : current.totalPrice();



        Bid updated = new Bid(

                current.id(),

                current.quoteRequestId(),

                current.artisanId(),

                pricePerUnit != null ? pricePerUnit : current.pricePerUnit(),

                newTotal,

                estimatedDays != null ? estimatedDays : current.estimatedDays(),

                note != null ? note : current.note(),

                current.deliveryTerms(),

                current.status(),

                current.createdAt(),

                Instant.now()

        );

        return bidRepository.save(updated);

    }



    public void acceptBid(Caller caller, UUID quoteId, UUID bidId) {

        QuoteRequest quote = getQuote(quoteId);

        checkBuyerOwnership(caller, quote.buyerId());



        if (quote.status() != QuoteStatus.SENT && quote.status() != QuoteStatus.BIDDING) {

            throw new ConflictException("Yêu cầu báo giá đã chốt hoặc không ở trạng thái nhận thầu");

        }



        Bid bid = bidRepository.findById(bidId)

                .orElseThrow(() -> new NotFoundException("Không tìm thấy giá thầu được chọn"));



        if (!bid.quoteRequestId().equals(quoteId)) {

            throw new InvalidInputException("Giá thầu không thuộc về yêu cầu báo giá này");

        }



        // Chấp thuận bid này

        Bid accepted = new Bid(

                bid.id(), bid.quoteRequestId(), bid.artisanId(), bid.pricePerUnit(),

                bid.totalPrice(), bid.estimatedDays(), bid.note(), bid.deliveryTerms(),

                BidStatus.ACCEPTED, bid.createdAt(), Instant.now()

        );

        bidRepository.save(accepted);



        // Từ chối tất cả các bid khác

        bidRepository.updateStatusForOtherBids(quoteId, bidId, BidStatus.REJECTED);



        // Chuyển QuoteRequest sang IN_PRODUCTION

        QuoteRequest updatedQuote = new QuoteRequest(

                quote.id(), quote.buyerId(), quote.categoryId(), quote.title(), quote.description(),

                quote.quantity(), quote.unit(), quote.deadline(), QuoteStatus.IN_PRODUCTION,

                quote.targetArtisanIds(), quote.deliveryNote(), quote.deliveryImages(),

                bidId, quote.version() + 1, quote.createdAt(), Instant.now()

        );

        quoteRepository.save(updatedQuote);

    }



    public void rejectBid(Caller caller, UUID quoteId, UUID bidId) {

        QuoteRequest quote = getQuote(quoteId);

        checkBuyerOwnership(caller, quote.buyerId());



        Bid bid = bidRepository.findById(bidId)

                .orElseThrow(() -> new NotFoundException("Không tìm thấy giá thầu"));



        Bid rejected = new Bid(

                bid.id(), bid.quoteRequestId(), bid.artisanId(), bid.pricePerUnit(),

                bid.totalPrice(), bid.estimatedDays(), bid.note(), bid.deliveryTerms(),

                BidStatus.REJECTED, bid.createdAt(), Instant.now()

        );

        bidRepository.save(rejected);

    }



    public QuoteRequest markDelivered(Caller caller, UUID quoteId, String deliveryNote, List<String> deliveryImages) {

        QuoteRequest quote = getQuote(quoteId);

        if (quote.status() != QuoteStatus.IN_PRODUCTION) {

            throw new ConflictException("Đơn hàng không ở trạng thái Đang sản xuất (InProduction)");

        }



        // Kiểm tra xem nghệ nhân trúng thầu có phải là caller này không

        if (quote.acceptedBidId() != null) {

            Bid bid = bidRepository.findById(quote.acceptedBidId()).orElse(null);

            if (bid != null) {

                checkArtisanOwnership(caller, bid.artisanId());

            }

        }



        QuoteRequest updated = new QuoteRequest(

                quote.id(), quote.buyerId(), quote.categoryId(), quote.title(), quote.description(),

                quote.quantity(), quote.unit(), quote.deadline(), QuoteStatus.DELIVERED,

                quote.targetArtisanIds(), deliveryNote, deliveryImages != null ? deliveryImages : List.of(),

                quote.acceptedBidId(), quote.version() + 1, quote.createdAt(), Instant.now()

        );

        return quoteRepository.save(updated);

    }



    public QuoteRequest confirmDelivery(Caller caller, UUID quoteId) {

        QuoteRequest quote = getQuote(quoteId);

        checkBuyerOwnership(caller, quote.buyerId());



        if (quote.status() != QuoteStatus.DELIVERED) {

            throw new ConflictException("Đơn hàng chưa được nghệ nhân đánh dấu giao hàng");

        }



        QuoteRequest updated = new QuoteRequest(

                quote.id(), quote.buyerId(), quote.categoryId(), quote.title(), quote.description(),

                quote.quantity(), quote.unit(), quote.deadline(), QuoteStatus.CLOSED,

                quote.targetArtisanIds(), quote.deliveryNote(), quote.deliveryImages(),

                quote.acceptedBidId(), quote.version() + 1, quote.createdAt(), Instant.now()

        );

        return quoteRepository.save(updated);

    }



    private QuoteRequest getQuote(UUID quoteId) {

        return quoteRepository.findById(quoteId)

                .orElseThrow(() -> new NotFoundException("Không tìm thấy yêu cầu báo giá"));

    }



    private void checkBuyerOwnership(Caller caller, UUID buyerId) {

        if (caller.isAdmin()) return;

        if (!caller.requireUserId().equals(buyerId)) {

            throw new ForbiddenException("Bạn không phải người tạo yêu cầu báo giá này");

        }

    }



    private void checkArtisanOwnership(Caller caller, UUID artisanId) {

        if (caller.isAdmin()) return;

        UUID userId = caller.requireUserId();

        ArtisanProfile artisan = artisanRepository.findByUserId(userId)

                .orElseThrow(() -> new ForbiddenException("Không tìm thấy hồ sơ xưởng gốm"));

        if (!artisan.id().equals(artisanId)) {

            throw new ForbiddenException("Bạn không có quyền thao tác trên giá thầu của xưởng khác");

        }

    }



    private QuoteStatus parseStatus(String status) {

        if (status == null || status.isBlank()) return null;

        try {

            return QuoteStatus.valueOf(status.toUpperCase());

        } catch (Exception e) {

            return null;

        }

    }

}

