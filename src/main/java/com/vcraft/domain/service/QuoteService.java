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
    public record BidAllocation(UUID bidId, int quantity) {}
    public record AllocationProgress(UUID parentId, QuoteStatus overallStatus, int totalQuantity,
            long acceptedQuantity, long deliveredQuantity, long confirmedQuantity,
            BigDecimal acceptedValue, List<QuoteRequest> parts) {}
    public record Allocation(UUID artisanId, int quantity) {}
    public record QuoteWithBids(QuoteRequest quote, List<Bid> bids) {}
    private final QuoteRepository quoteRepository;
    private final BidRepository bidRepository;
    private final ArtisanRepository artisanRepository;
    private com.vcraft.domain.repository.ProductRepository products;
    public QuoteService(QuoteRepository quoteRepository,
                        BidRepository bidRepository,
                        ArtisanRepository artisanRepository) {
        this.quoteRepository = quoteRepository;
        this.bidRepository = bidRepository;
        this.artisanRepository = artisanRepository;
    }
    public QuoteService(QuoteRepository quotes, BidRepository bids, ArtisanRepository artisans,
                        com.vcraft.domain.repository.ProductRepository products) {
        this(quotes, bids, artisans);
        this.products = products;
    }
    public List<QuoteRequest> getMyQuotes(Caller caller, String status, int page, int limit) {
        requireBuyer(caller);
        UUID buyerId = caller.requireUserId();
        QuoteStatus quoteStatus = parseStatus(status);
        return quoteRepository.findByBuyerId(buyerId, quoteStatus, page, limit);
    }
    public long countMyQuotes(Caller caller, String status) {
        requireBuyer(caller);
        UUID buyerId = caller.requireUserId();
        QuoteStatus quoteStatus = parseStatus(status);
        return quoteRepository.countByBuyerId(buyerId, quoteStatus);
    }
    public QuoteRequest createQuote(Caller caller, String title, String description, int quantity,
                                    String unit, Instant deadline, UUID categoryId, List<UUID> targetArtisanIds) {
        requireBuyer(caller);
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
        if (title == null || title.isBlank() || description == null || description.isBlank() || quantity <= 0
                || (deadline != null && !deadline.isAfter(Instant.now())))
            throw new InvalidInputException("Tiêu đề, mô tả, số lượng hoặc hạn giao không hợp lệ");
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
                if (quote.status() == QuoteStatus.DRAFT || quote.status() == QuoteStatus.ALLOCATED) {
                    throw new ForbiddenException("Yêu cầu báo giá đang ở trạng thái bản nháp của khách hàng");
                }
                ArtisanProfile artisan = artisanRepository.findByUserId(userId)
                        .orElseThrow(() -> new ForbiddenException("Chưa có hồ sơ nghệ nhân"));
                if (!artisan.isApproved()) throw new ForbiddenException("Xưởng chưa được duyệt");
                if (quote.targetArtisanIds() != null && !quote.targetArtisanIds().isEmpty()
                        && !quote.targetArtisanIds().contains(artisan.id())) {
                    throw new ForbiddenException("Yêu cầu báo giá này không được gửi tới xưởng của bạn");
                }
            } else {
                throw new ForbiddenException("Bạn không có quyền xem yêu cầu báo giá này");
            }
        }
        List<Bid> bids = bidRepository.findByQuoteRequestId(quoteId);
        // Bảo mật giá thầu: Nghệ nhân chỉ thấy giá thầu của xưởng mình
        if (caller.isArtisan() && !caller.isAdmin()) {
            UUID userId = caller.requireUserId();
            ArtisanProfile artisan = artisanRepository.findByUserId(userId).orElse(null);
            if (artisan != null) {
                bids = bids.stream()
                        .filter(b -> b.artisanId().equals(artisan.id()))
                        .toList();
            } else {
                bids = List.of();
            }
        }
        return new QuoteWithBids(quote, bids);
    }
    public QuoteRequest updateDraftQuote(Caller caller, UUID quoteId, String title,
                                         String description, Integer quantity, Instant deadline) {
        QuoteRequest current = getQuote(quoteId);
        checkBuyerOwnership(caller, current.buyerId());
        if (current.status() != QuoteStatus.DRAFT) {
            throw new ConflictException("Chỉ có thể chỉnh sửa yêu cầu báo giá khi còn ở trạng thái Bản nháp (Draft)");
        }
        if ((quantity != null && quantity <= 0) || (title != null && title.isBlank())
                || (description != null && description.isBlank()) || (deadline != null && !deadline.isAfter(Instant.now())))
            throw new InvalidInputException("Dữ liệu yêu cầu không hợp lệ");
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
        if (!current.deadline().isAfter(Instant.now())) throw new InvalidInputException("Hạn giao đã qua");
        List<UUID> recipients = current.targetArtisanIds();
        if (recipients == null || recipients.isEmpty()) {
            var matched = new java.util.ArrayList<UUID>();
            for (int page = 1; ; page++) {
                var artisans = artisanRepository.findAllApproved(page, 100, null, null);
                for (var artisan : artisans) {
                    if (current.categoryId() == null || products == null
                            || products.countProducts(artisan.id(), current.categoryId(), null, null) > 0)
                        matched.add(artisan.id());
                }
                if (artisans.size() < 100) break;
            }
            recipients = matched;
        }
        if (recipients.isEmpty()) throw new InvalidInputException("Không tìm thấy xưởng phù hợp; chọn xưởng nhận yêu cầu");
        for (var id : recipients) {
            var recipient = artisanRepository.findById(id).orElseThrow(() -> new InvalidInputException("Xưởng nhận không tồn tại"));
            if (!recipient.isApproved()) throw new InvalidInputException("Xưởng nhận chưa được duyệt");
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
                recipients,
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
        if (!artisan.isApproved()) throw new ForbiddenException("Xưởng chưa được duyệt");
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
        if (quote.targetArtisanIds() != null && !quote.targetArtisanIds().isEmpty()
                && !quote.targetArtisanIds().contains(artisan.id())) {
            throw new ForbiddenException("Yêu cầu không được gửi đến xưởng của bạn");
        }
        if (!quote.deadline().isAfter(Instant.now())) throw new ConflictException("Yêu cầu đã quá hạn giao");
        validateBid(pricePerUnit, estimatedDays);
        if (totalPrice != null && totalPrice.compareTo(pricePerUnit.multiply(BigDecimal.valueOf(quote.quantity()))) != 0) {
            throw new InvalidInputException("Tổng giá phải bằng đơn giá nhân số lượng");
        }
        com.vcraft.domain.model.Money.positive(pricePerUnit.multiply(BigDecimal.valueOf(quote.quantity())));
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
        QuoteRequest quote = getQuote(quoteId);
        Bid current = bidRepository.findById(bidId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giá thầu"));
        checkArtisanOwnership(caller, current.artisanId());
        if (current.status() != BidStatus.PENDING) {
            throw new ConflictException("Không thể sửa giá thầu đã được chấp thuận hoặc từ chối");
        }
        requirePendingBid(quote, current);
        validateBid(pricePerUnit != null ? pricePerUnit : current.pricePerUnit(),
                estimatedDays != null ? estimatedDays : current.estimatedDays());
        BigDecimal newTotal = pricePerUnit != null ? pricePerUnit.multiply(BigDecimal.valueOf(quote.quantity())) : current.totalPrice();
        com.vcraft.domain.model.Money.positive(newTotal);
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
        requirePendingBid(quote, bid);
        // Chấp thuận bid này
        Bid accepted = new Bid(
                bid.id(), bid.quoteRequestId(), bid.artisanId(), bid.pricePerUnit(),
                bid.totalPrice(), bid.estimatedDays(), bid.note(), bid.deliveryTerms(),
                BidStatus.ACCEPTED, bid.createdAt(), Instant.now()
        );
        bidRepository.save(accepted);
        // Từ chối tất cả các bid khác
        bidRepository.updateStatusForOtherBids(quoteId, bidId, BidStatus.REJECTED);
        // Buyer accepts; workshop must explicitly start production.
        QuoteRequest updatedQuote = new QuoteRequest(
                quote.id(), quote.buyerId(), quote.categoryId(), quote.title(), quote.description(),
                quote.quantity(), quote.unit(), quote.deadline(), QuoteStatus.ACCEPTED,
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
        requirePendingBid(quote, bid);
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
        checkWinningArtisan(caller, quote);
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
    /** Split an unawarded request into independently bid, buyer-approved workshop lots. */
    public List<QuoteRequest> allocate(Caller caller, UUID quoteId, List<Allocation> parts) {
        caller.requireAdmin();
        QuoteRequest parent = getQuote(quoteId);
        requireRoot(parent.id());
        if (parent.status() != QuoteStatus.DRAFT && parent.status() != QuoteStatus.SENT)
            throw new ConflictException("Chỉ điều phối yêu cầu chưa có chào giá");
        if (!bidRepository.findByQuoteRequestId(quoteId).isEmpty())
            throw new ConflictException("Không chia yêu cầu đã có chào giá");
        if (parts == null || parts.size() < 2 || parts.stream().anyMatch(p -> p == null || p.quantity() <= 0 || p.artisanId() == null)
                || parts.stream().mapToLong(Allocation::quantity).sum() != parent.quantity()
                || parts.stream().map(Allocation::artisanId).distinct().count() != parts.size())
            throw new InvalidInputException("Cần ít nhất hai xưởng khác nhau; tổng số lượng phải bằng yêu cầu gốc");
        if (!parent.deadline().isAfter(Instant.now())) throw new InvalidInputException("Hạn giao đã qua");
        for (var part : parts) {
            var artisan = artisanRepository.findById(part.artisanId())
                    .orElseThrow(() -> new InvalidInputException("Không tìm thấy xưởng được phân bổ"));
            if (!artisan.isApproved()) throw new InvalidInputException("Xưởng phải được duyệt");
        }
        var children = new java.util.ArrayList<QuoteRequest>();
        for (var part : parts) {
            var child = quoteRepository.save(new QuoteRequest(UUID.randomUUID(), parent.buyerId(), parent.categoryId(),
                    parent.title(), parent.description(), part.quantity(), parent.unit(), parent.deadline(), QuoteStatus.SENT,
                    List.of(part.artisanId()), null, List.of(), null, 0, Instant.now(), Instant.now()));
            quoteRepository.linkAllocation(parent.id(), child.id());
            children.add(child);
        }
        quoteRepository.save(new QuoteRequest(parent.id(), parent.buyerId(), parent.categoryId(), parent.title(),
                parent.description(), parent.quantity(), parent.unit(), parent.deadline(), QuoteStatus.ALLOCATED,
                parent.targetArtisanIds(), parent.deliveryNote(), parent.deliveryImages(), null,
                parent.version() + 1, parent.createdAt(), Instant.now()));
        return children;
    }

    /** Buyer explicitly accepts quantities at the quoted unit prices; no production is started implicitly. */
    public List<QuoteRequest> acceptAllocations(Caller caller, UUID quoteId, List<BidAllocation> parts) {
        QuoteRequest parent = getQuote(quoteId);
        checkBuyerOwnership(caller, parent.buyerId());
        requireRoot(parent.id());
        if (!parent.canReceiveBids()) throw new ConflictException("Yêu cầu không còn nhận chào giá");
        if (parts == null || parts.size() < 2 || parts.stream().anyMatch(p -> p == null || p.bidId() == null || p.quantity() <= 0)
                || parts.stream().mapToLong(BidAllocation::quantity).sum() != parent.quantity()
                || parts.stream().map(BidAllocation::bidId).distinct().count() != parts.size())
            throw new InvalidInputException("Cần ít nhất hai giá thầu khác nhau; tổng số lượng phải bằng đơn gốc");
        var selected = new java.util.LinkedHashMap<UUID, Bid>();
        for (var part : parts) {
            Bid bid = bidRepository.findById(part.bidId()).orElseThrow(() -> new NotFoundException("Không tìm thấy giá thầu"));
            requirePendingBid(parent, bid);
            var artisan = artisanRepository.findById(bid.artisanId()).filter(ArtisanProfile::isApproved)
                    .orElseThrow(() -> new ConflictException("Xưởng được chọn chưa được duyệt hoặc đã bị khóa"));
            if (selected.values().stream().anyMatch(b -> b.artisanId().equals(artisan.id())))
                throw new InvalidInputException("Mỗi xưởng chỉ được phân bổ một phần");
            selected.put(bid.id(), bid);
        }
        var children = new java.util.ArrayList<QuoteRequest>();
        for (var part : parts) {
            Bid source = selected.get(part.bidId());
            UUID childId = UUID.randomUUID(), bidId = UUID.randomUUID();
            // Insert child first so bid FK is valid, then point it at its accepted bid.
            var child = quoteRepository.save(new QuoteRequest(childId, parent.buyerId(), parent.categoryId(), parent.title(),
                    parent.description(), part.quantity(), parent.unit(), parent.deadline(), QuoteStatus.SENT,
                    List.of(source.artisanId()), null, List.of(), null, 0, Instant.now(), Instant.now()));
            bidRepository.save(new Bid(bidId, childId, source.artisanId(), source.pricePerUnit(),
                    source.pricePerUnit().multiply(BigDecimal.valueOf(part.quantity())), source.estimatedDays(),
                    source.note(), source.deliveryTerms(), BidStatus.ACCEPTED, Instant.now(), Instant.now()));
            child = quoteRepository.save(new QuoteRequest(child.id(), child.buyerId(), child.categoryId(), child.title(),
                    child.description(), child.quantity(), child.unit(), child.deadline(), QuoteStatus.ACCEPTED,
                    child.targetArtisanIds(), null, List.of(), bidId, 1, child.createdAt(), Instant.now()));
            quoteRepository.linkAllocation(parent.id(), child.id());
            children.add(child);
        }
        for (Bid original : bidRepository.findByQuoteRequestId(quoteId)) {
            if (original.status() != BidStatus.PENDING) continue;
            bidRepository.save(new Bid(original.id(), original.quoteRequestId(), original.artisanId(), original.pricePerUnit(),
                    original.totalPrice(), original.estimatedDays(), original.note(), original.deliveryTerms(),
                    selected.containsKey(original.id()) ? BidStatus.ACCEPTED : BidStatus.REJECTED, original.createdAt(), Instant.now()));
        }
        quoteRepository.save(new QuoteRequest(parent.id(), parent.buyerId(), parent.categoryId(), parent.title(),
                parent.description(), parent.quantity(), parent.unit(), parent.deadline(), QuoteStatus.ALLOCATED,
                parent.targetArtisanIds(), parent.deliveryNote(), parent.deliveryImages(), null,
                parent.version() + 1, parent.createdAt(), Instant.now()));
        return children;
    }

    public AllocationProgress getAllocationProgress(Caller caller, UUID id) {
        var parent = getQuote(id);
        checkBuyerOwnership(caller, parent.buyerId());
        var parts = quoteRepository.findAllocations(id);
        if (parent.status() != QuoteStatus.ALLOCATED || parts.isEmpty()) throw new ConflictException("Yêu cầu chưa được điều phối");
        if (parts.stream().mapToLong(QuoteRequest::quantity).sum() != parent.quantity())
            throw new ConflictException("Tổng phân bổ không khớp yêu cầu gốc");
        long accepted = 0, delivered = 0, confirmed = 0;
        BigDecimal value = BigDecimal.ZERO;
        boolean started = false, bidding = false;
        for (var part : parts) {
            var status = part.status();
            if (part.acceptedBidId() != null) {
                accepted += part.quantity();
                var bid = bidRepository.findById(part.acceptedBidId()).orElseThrow(() -> new ConflictException("Thiếu bid đã nhận"));
                value = value.add(bid.totalPrice());
            }
            if (status == QuoteStatus.DELIVERED || status == QuoteStatus.CLOSED) delivered += part.quantity();
            if (status == QuoteStatus.CLOSED) confirmed += part.quantity();
            started |= status == QuoteStatus.IN_PRODUCTION || status == QuoteStatus.DELIVERED || status == QuoteStatus.CLOSED;
            bidding |= status == QuoteStatus.BIDDING;
        }
        QuoteStatus overall = confirmed == parent.quantity() ? QuoteStatus.CLOSED
                : delivered == parent.quantity() ? QuoteStatus.DELIVERED : started ? QuoteStatus.IN_PRODUCTION
                : accepted == parent.quantity() ? QuoteStatus.ACCEPTED : bidding || accepted > 0 ? QuoteStatus.BIDDING : QuoteStatus.SENT;
        return new AllocationProgress(id, overall, parent.quantity(), accepted, delivered, confirmed, value, parts);
    }

    private void requireRoot(UUID id) {
        if (quoteRepository.findParentId(id).isPresent()) throw new ConflictException("Không chia tiếp một phần đã được phân bổ");
    }

    public List<QuoteRequest> getAllocations(Caller caller, UUID quoteId) {
        var parent = getQuote(quoteId);
        checkBuyerOwnership(caller, parent.buyerId());
        return quoteRepository.findAllocations(quoteId);
    }

    public QuoteRequest startProduction(Caller caller, UUID quoteId) {
        QuoteRequest quote = getQuote(quoteId);
        if (quote.status() != QuoteStatus.ACCEPTED) {
            throw new ConflictException("Chỉ bắt đầu sản xuất sau khi buyer chấp nhận");
        }
        checkWinningArtisan(caller, quote);
        return quoteRepository.save(new QuoteRequest(
                quote.id(), quote.buyerId(), quote.categoryId(), quote.title(), quote.description(),
                quote.quantity(), quote.unit(), quote.deadline(), QuoteStatus.IN_PRODUCTION,
                quote.targetArtisanIds(), quote.deliveryNote(), quote.deliveryImages(),
                quote.acceptedBidId(), quote.version() + 1, quote.createdAt(), Instant.now()));
    }

    private void checkWinningArtisan(Caller caller, QuoteRequest quote) {
        if (quote.acceptedBidId() == null) throw new ConflictException("Chưa có xưởng trúng thầu");
        Bid bid = bidRepository.findById(quote.acceptedBidId())
                .orElseThrow(() -> new ConflictException("Không tìm thấy giá thầu đã chấp nhận"));
        if (!bid.quoteRequestId().equals(quote.id()) || bid.status() != BidStatus.ACCEPTED)
            throw new ConflictException("Giá thầu được chấp nhận không hợp lệ");
        checkArtisanOwnership(caller, bid.artisanId());
    }

    private void requirePendingBid(QuoteRequest quote, Bid bid) {
        if (!bid.quoteRequestId().equals(quote.id()))
            throw new InvalidInputException("Giá thầu không thuộc yêu cầu này");
        if (!quote.deadline().isAfter(Instant.now())) throw new ConflictException("Yêu cầu đã quá hạn giao");
        if (!quote.canReceiveBids() || bid.status() != BidStatus.PENDING)
            throw new ConflictException("Yêu cầu đã chốt hoặc giá thầu không còn chờ xử lý");
    }

    private void validateBid(BigDecimal price, int days) {
        com.vcraft.domain.model.Money.positive(price);
        if (price == null || price.signum() <= 0 || days <= 0)
            throw new InvalidInputException("Đơn giá và thời gian hoàn thành phải lớn hơn 0");
    }

    private void requireBuyer(Caller caller) {
        caller.requireUserId();
        if (!caller.isBuyer() && !caller.isAdmin()) throw new ForbiddenException("Chỉ khách hàng được tạo yêu cầu");
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
            throw new InvalidInputException("Trạng thái báo giá không hợp lệ");
        }
    }
}
