package com.vcraft.api.quote;

import com.vcraft.api.CallerResolver;
import com.vcraft.api.dto.PageResponse;
import com.vcraft.api.quote.dto.CompleteDeliveryRequest;
import com.vcraft.api.quote.dto.CreateQuoteRequest;
import com.vcraft.api.quote.dto.PlaceBidRequest;
import com.vcraft.api.quote.dto.UpdateQuoteRequest;
import com.vcraft.domain.model.Bid;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.QuoteRequest;
import com.vcraft.domain.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quotes")
@Tag(name = "7. Quote Requests & Bidding", description = "Các API luồng yêu cầu báo giá và đấu thầu sỉ (State Machine)")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping
    @Operation(summary = "Buyer xem danh sách yêu cầu báo giá của mình")
    public ResponseEntity<PageResponse<QuoteRequest>> getMyQuotes(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status
    ) {
        Caller caller = CallerResolver.resolve(authentication);
        List<QuoteRequest> data = quoteService.getMyQuotes(caller, status, page, limit);
        long total = quoteService.countMyQuotes(caller, status);
        return ResponseEntity.ok(PageResponse.of(data, page, limit, total));
    }

    @PostMapping
    @Operation(summary = "Buyer tạo yêu cầu báo giá mới (Trạng thái khởi tạo: Draft)")
    public ResponseEntity<QuoteRequest> createQuote(Authentication authentication,
                                                    @Valid @RequestBody CreateQuoteRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        QuoteRequest quote = quoteService.createQuote(
                caller, request.title(), request.description(), request.quantity(),
                request.unit(), request.deadline(), request.categoryId(), request.targetArtisanIds()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(quote);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết một yêu cầu báo giá (kèm danh sách giá thầu từ các nghệ nhân)")
    public ResponseEntity<QuoteService.QuoteWithBids> getQuoteDetail(Authentication authentication,
                                                                    @PathVariable UUID id) {
        Caller caller = CallerResolver.resolve(authentication);
        return ResponseEntity.ok(quoteService.getQuoteDetail(caller, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Buyer chỉnh sửa yêu cầu báo giá khi còn ở trạng thái Bản nháp (Draft)")
    public ResponseEntity<QuoteRequest> updateDraftQuote(Authentication authentication,
                                                         @PathVariable UUID id,
                                                         @RequestBody UpdateQuoteRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        QuoteRequest updated = quoteService.updateDraftQuote(
                caller, id, request.title(), request.description(), request.quantity(), request.deadline()
        );
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/submit")
    @Operation(summary = "Buyer phát hành/gửi yêu cầu báo giá tới các nghệ nhân (Draft -> Sent)")
    public ResponseEntity<QuoteRequest> submitQuote(Authentication authentication,
                                                   @PathVariable UUID id) {
        Caller caller = CallerResolver.resolve(authentication);
        return ResponseEntity.ok(quoteService.submitQuote(caller, id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Buyer hủy yêu cầu báo giá khi còn ở trạng thái Draft")
    public ResponseEntity<Map<String, String>> deleteQuote(Authentication authentication,
                                                           @PathVariable UUID id) {
        Caller caller = CallerResolver.resolve(authentication);
        quoteService.deleteDraftQuote(caller, id);
        return ResponseEntity.ok(Map.of("message", "Deleted quote request successfully"));
    }

    @GetMapping("/artisan/incoming")
    @Operation(summary = "Nghệ nhân xem danh sách các yêu cầu báo giá gửi đến xưởng")
    public ResponseEntity<List<QuoteRequest>> getIncomingQuotes(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status
    ) {
        Caller caller = CallerResolver.resolve(authentication);
        return ResponseEntity.ok(quoteService.getIncomingQuotesForArtisan(caller, status, page, limit));
    }

    @PostMapping("/{id}/bids")
    @Operation(summary = "Nghệ nhân đặt giá chào thầu cho yêu cầu (Bidding)")
    public ResponseEntity<Bid> placeBid(Authentication authentication,
                                        @PathVariable UUID id,
                                        @Valid @RequestBody PlaceBidRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        Bid bid = quoteService.placeBid(
                caller, id, request.pricePerUnit(), request.totalPrice(),
                request.estimatedDays(), request.note(), request.deliveryTerms()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(bid);
    }

    @PutMapping("/{id}/bids/{bidId}")
    @Operation(summary = "Nghệ nhân chỉnh sửa giá đã đặt")
    public ResponseEntity<Bid> updateBid(Authentication authentication,
                                         @PathVariable UUID id,
                                         @PathVariable UUID bidId,
                                         @RequestBody PlaceBidRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        Bid updated = quoteService.updateBid(caller, id, bidId, request.pricePerUnit(), request.estimatedDays(), request.note());
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/bids/{bidId}/accept")
    @Operation(summary = "Buyer chọn xưởng gốm thắng thầu (Bid -> Accepted, Quote -> InProduction, các Bid khác -> Rejected)")
    public ResponseEntity<Map<String, String>> acceptBid(Authentication authentication,
                                                         @PathVariable UUID id,
                                                         @PathVariable UUID bidId) {
        Caller caller = CallerResolver.resolve(authentication);
        quoteService.acceptBid(caller, id, bidId);
        return ResponseEntity.ok(Map.of("message", "Đã chấp thuận giá thầu. Đơn hàng chuyển sang trạng thái sản xuất."));
    }

    @PatchMapping("/{id}/bids/{bidId}/reject")
    @Operation(summary = "Buyer từ chối một giá thầu")
    public ResponseEntity<Map<String, String>> rejectBid(Authentication authentication,
                                                         @PathVariable UUID id,
                                                         @PathVariable UUID bidId) {
        Caller caller = CallerResolver.resolve(authentication);
        quoteService.rejectBid(caller, id, bidId);
        return ResponseEntity.ok(Map.of("message", "Đã từ chối giá thầu."));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Nghệ nhân đánh dấu đã hoàn thành và giao hàng (InProduction -> Delivered)")
    public ResponseEntity<QuoteRequest> markDelivered(Authentication authentication,
                                                      @PathVariable UUID id,
                                                      @RequestBody CompleteDeliveryRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        QuoteRequest updated = quoteService.markDelivered(caller, id, request.deliveryNote(), request.deliveryImages());
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/confirm-delivery")
    @Operation(summary = "Buyer xác nhận đã nhận hàng thành công (Delivered -> Closed)")
    public ResponseEntity<QuoteRequest> confirmDelivery(Authentication authentication,
                                                        @PathVariable UUID id) {
        Caller caller = CallerResolver.resolve(authentication);
        return ResponseEntity.ok(quoteService.confirmDelivery(caller, id));
    }
}
