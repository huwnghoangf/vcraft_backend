package com.vcraft.domain.service;



import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.mock;

import static org.mockito.Mockito.never;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;



import com.vcraft.domain.exception.ConflictException;

import com.vcraft.domain.exception.ForbiddenException;

import com.vcraft.domain.exception.NotFoundException;

import com.vcraft.domain.model.ArtisanProfile;

import com.vcraft.domain.model.ArtisanStatus;

import com.vcraft.domain.model.Bid;

import com.vcraft.domain.model.BidStatus;

import com.vcraft.domain.model.Caller;

import com.vcraft.domain.model.QuoteRequest;

import com.vcraft.domain.model.QuoteStatus;

import com.vcraft.domain.model.Role;

import com.vcraft.domain.repository.ArtisanRepository;

import com.vcraft.domain.repository.BidRepository;

import com.vcraft.domain.repository.QuoteRepository;

import java.math.BigDecimal;

import java.time.Instant;

import java.util.List;

import java.util.Optional;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;



class QuoteServiceTest {



    private static final UUID BUYER_ID = UUID.randomUUID();

    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    private static final UUID ARTISAN_USER_ID = UUID.randomUUID();

    private static final UUID ARTISAN_ID = UUID.randomUUID();

    private static final UUID QUOTE_ID = UUID.randomUUID();

    private static final UUID BID_ID = UUID.randomUUID();



    private QuoteRepository quoteRepository;

    private BidRepository bidRepository;

    private ArtisanRepository artisanRepository;

    private QuoteService quoteService;



    private QuoteRequest draftQuote;

    private QuoteRequest biddingQuote;

    private ArtisanProfile artisanProfile;

    private Bid pendingBid;



    @BeforeEach

    void setUp() {

        quoteRepository = mock(QuoteRepository.class);

        bidRepository = mock(BidRepository.class);

        artisanRepository = mock(ArtisanRepository.class);



        quoteService = new QuoteService(quoteRepository, bidRepository, artisanRepository);



        draftQuote = new QuoteRequest(

                QUOTE_ID,

                BUYER_ID,

                UUID.randomUUID(),

                "Đặt 500 ấm trà Bát Tràng",

                "Yêu cầu men rạn cổ truyền",

                500,

                "bộ",

                Instant.now().plusSeconds(86400 * 30),

                QuoteStatus.DRAFT,

                List.of(),

                null,

                List.of(),

                null,

                0,

                Instant.now(),

                Instant.now()

        );



        biddingQuote = new QuoteRequest(

                QUOTE_ID,

                BUYER_ID,

                UUID.randomUUID(),

                "Đặt 500 ấm trà Bát Tràng",

                "Yêu cầu men rạn cổ truyền",

                500,

                "bộ",

                Instant.now().plusSeconds(86400 * 30),

                QuoteStatus.BIDDING,

                List.of(),

                null,

                List.of(),

                null,

                1,

                Instant.now(),

                Instant.now()

        );



        artisanProfile = new ArtisanProfile(

                ARTISAN_ID,

                ARTISAN_USER_ID,

                "Xưởng Gốm Hùng Thịnh",

                "Lê Văn Hùng",

                "0987654321",

                "Bát Tràng",

                "Gia Lâm, Hà Nội",

                20.97,

                105.91,

                true,

                "Xưởng gốm cổ truyền",

                null,

                List.of(),

                List.of(),

                ArtisanStatus.APPROVED,

                null,

                Instant.now(),

                Instant.now()

        );



        pendingBid = new Bid(

                BID_ID,

                QUOTE_ID,

                ARTISAN_ID,

                new BigDecimal("250000"),

                new BigDecimal("125000000"),

                25,

                "Men rạn chuẩn",

                "Giao tận nơi",

                BidStatus.PENDING,

                Instant.now(),

                Instant.now()

        );

    }



    @Test
    @DisplayName("Nghệ nhân khi xem báo giá chỉ thấy giá thầu của xưởng mình, không thấy giá đối thủ")
    void getQuoteDetailFiltersOutCompetitorBidsForArtisan() {
        UUID rivalArtisanId = UUID.randomUUID();
        Bid rivalBid = new Bid(
                UUID.randomUUID(), QUOTE_ID, rivalArtisanId,
                new BigDecimal("180000"), new BigDecimal("90000000"),
                15, "Giá cực rẻ đối thủ", "Giao nhanh",
                BidStatus.PENDING, Instant.now(), Instant.now()
        );

        when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(biddingQuote));
        when(artisanRepository.findByUserId(ARTISAN_USER_ID)).thenReturn(Optional.of(artisanProfile));
        when(bidRepository.findByQuoteRequestId(QUOTE_ID)).thenReturn(List.of(pendingBid, rivalBid));

        Caller artisanCaller = Caller.authenticated(ARTISAN_USER_ID, "artisan@vcraft.vn", Role.ARTISAN);

        QuoteService.QuoteWithBids result = quoteService.getQuoteDetail(artisanCaller, QUOTE_ID);

        assertThat(result.bids()).hasSize(1);
        assertThat(result.bids().get(0).artisanId()).isEqualTo(ARTISAN_ID);
    }

    @Test
    @DisplayName("Người mua xem chi tiết báo giá của người mua khác phải ném ForbiddenException")
    void getQuoteDetailOfOtherBuyerThrowsForbidden() {

        when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(draftQuote));

        Caller stranger = Caller.authenticated(OTHER_USER_ID, "stranger@vcraft.vn", Role.BUYER);



        assertThatThrownBy(() -> quoteService.getQuoteDetail(stranger, QUOTE_ID))

                .isInstanceOf(ForbiddenException.class)

                .hasMessageContaining("Bạn không có quyền xem yêu cầu báo giá của người khác");

    }



    @Test

    @DisplayName("Sửa yêu cầu báo giá của người khác phải ném ForbiddenException")

    void updateQuoteOfOtherUserThrowsForbidden() {

        when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(draftQuote));

        Caller stranger = Caller.authenticated(OTHER_USER_ID, "stranger@vcraft.vn", Role.BUYER);



        assertThatThrownBy(() -> quoteService.updateDraftQuote(

                stranger, QUOTE_ID, "Tiêu đề mới", "Mô tả mới", 1000, null))

                .isInstanceOf(ForbiddenException.class)

                .hasMessageContaining("Bạn không phải người tạo yêu cầu báo giá này");



        verify(quoteRepository, never()).save(any());

    }



    @Test

    @DisplayName("Sửa yêu cầu báo giá khi đã được gửi (không còn DRAFT) phải ném ConflictException")

    void updateQuoteWhenNotDraftThrowsConflict() {

        when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(biddingQuote));

        Caller owner = Caller.authenticated(BUYER_ID, "buyer@vcraft.vn", Role.BUYER);



        assertThatThrownBy(() -> quoteService.updateDraftQuote(

                owner, QUOTE_ID, "Tiêu đề mới", "Mô tả mới", 1000, null))

                .isInstanceOf(ConflictException.class)

                .hasMessageContaining("Bản nháp");



        verify(quoteRepository, never()).save(any());

    }



    @Test

    @DisplayName("Chào giá cho yêu cầu còn ở trạng thái DRAFT phải ném ConflictException")

    void placeBidOnDraftQuoteThrowsConflict() {

        when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(draftQuote));

        Caller artisanCaller = Caller.authenticated(ARTISAN_USER_ID, "artisan@vcraft.vn", Role.ARTISAN);



        assertThatThrownBy(() -> quoteService.placeBid(

                artisanCaller, QUOTE_ID, new BigDecimal("200000"), null, 20, "Chào giá", "Giao hàng"))

                .isInstanceOf(ConflictException.class)

                .hasMessageContaining("không trong thời gian tiếp nhận chào giá");



        verify(bidRepository, never()).save(any());

    }



    @Test

    @DisplayName("Nghệ nhân trạng thái PENDING khi chào giá phải ném ForbiddenException")

    void placeBidWithPendingArtisanThrowsForbidden() {

        ArtisanProfile pendingProfile = new ArtisanProfile(

                ARTISAN_ID, ARTISAN_USER_ID, "Xưởng Chờ Duyệt", "Lê Văn Hùng", "0987654321",

                "Bát Tràng", "Gia Lâm, Hà Nội", 20.97, 105.91, true, "Xưởng gốm", null,

                List.of(), List.of(), ArtisanStatus.PENDING, null, Instant.now(), Instant.now()

        );

        when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(biddingQuote));

        when(artisanRepository.findByUserId(ARTISAN_USER_ID)).thenReturn(Optional.of(pendingProfile));



        Caller artisanCaller = Caller.authenticated(ARTISAN_USER_ID, "artisan@vcraft.vn", Role.ARTISAN);



        assertThatThrownBy(() -> quoteService.placeBid(

                artisanCaller, QUOTE_ID, new BigDecimal("200000"), null, 20, "Chào giá", "Giao hàng"))

                .isInstanceOf(ForbiddenException.class)

                .hasMessageContaining("đang chờ duyệt hoặc đã bị khóa");



        verify(bidRepository, never()).save(any());

    }



    @Test

    @DisplayName("Nghệ nhân chưa có hồ sơ khi chào giá phải ném ForbiddenException")

    void placeBidWithoutArtisanProfileThrowsForbidden() {

        when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(biddingQuote));

        when(artisanRepository.findByUserId(ARTISAN_USER_ID)).thenReturn(Optional.empty());



        Caller artisanCaller = Caller.authenticated(ARTISAN_USER_ID, "artisan@vcraft.vn", Role.ARTISAN);



        assertThatThrownBy(() -> quoteService.placeBid(

                artisanCaller, QUOTE_ID, new BigDecimal("200000"), null, 20, "Chào giá", "Giao hàng"))

                .isInstanceOf(ForbiddenException.class)

                .hasMessageContaining("Chỉ nghệ nhân mới được đặt giá thầu");



        verify(bidRepository, never()).save(any());

    }



    @Test

    @DisplayName("Chào giá lần hai khi đã có giá thầu trước đó phải ném ConflictException")

    void placeDuplicateBidThrowsConflict() {

        when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(biddingQuote));

        when(artisanRepository.findByUserId(ARTISAN_USER_ID)).thenReturn(Optional.of(artisanProfile));

        when(bidRepository.findByQuoteRequestIdAndArtisanId(QUOTE_ID, ARTISAN_ID)).thenReturn(Optional.of(pendingBid));



        Caller artisanCaller = Caller.authenticated(ARTISAN_USER_ID, "artisan@vcraft.vn", Role.ARTISAN);



        assertThatThrownBy(() -> quoteService.placeBid(

                artisanCaller, QUOTE_ID, new BigDecimal("200000"), null, 20, "Chào giá", "Giao hàng"))

                .isInstanceOf(ConflictException.class)

                .hasMessageContaining("Bạn đã gửi chào giá cho yêu cầu này rồi");



        verify(bidRepository, never()).save(any());

    }



    @Test

    @DisplayName("Người lạ chấp nhận giá thầu của yêu cầu báo giá người khác phải ném ForbiddenException")

    void acceptBidByStrangerThrowsForbidden() {

        when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(biddingQuote));

        Caller stranger = Caller.authenticated(OTHER_USER_ID, "stranger@vcraft.vn", Role.BUYER);



        assertThatThrownBy(() -> quoteService.acceptBid(stranger, QUOTE_ID, BID_ID))

                .isInstanceOf(ForbiddenException.class)

                .hasMessageContaining("Bạn không phải người tạo yêu cầu báo giá này");

    }



    @Test

    @DisplayName("Chấp nhận giá thầu khi yêu cầu báo giá không tồn tại phải ném NotFoundException")

    void acceptBidOnNonExistentQuoteThrowsNotFound() {

        UUID nonExistent = UUID.randomUUID();

        when(quoteRepository.findById(nonExistent)).thenReturn(Optional.empty());

        Caller owner = Caller.authenticated(BUYER_ID, "buyer@vcraft.vn", Role.BUYER);



        assertThatThrownBy(() -> quoteService.acceptBid(owner, nonExistent, BID_ID))

                .isInstanceOf(NotFoundException.class)

                .hasMessageContaining("Không tìm thấy yêu cầu báo giá");

    }

}

