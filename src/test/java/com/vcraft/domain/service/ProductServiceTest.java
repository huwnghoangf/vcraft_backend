package com.vcraft.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vcraft.domain.exception.ForbiddenException;
import com.vcraft.domain.model.ArtisanProfile;
import com.vcraft.domain.model.ArtisanStatus;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.Product;
import com.vcraft.domain.model.Role;
import com.vcraft.domain.repository.ArtisanRepository;
import com.vcraft.domain.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ARTISAN_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    private ProductRepository productRepository;
    private ArtisanRepository artisanRepository;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        artisanRepository = mock(ArtisanRepository.class);
        productService = new ProductService(productRepository, artisanRepository);
    }

    private ArtisanProfile createProfile(ArtisanStatus status) {
        return new ArtisanProfile(
                ARTISAN_ID,
                USER_ID,
                "Xưởng Gốm Minh Long",
                "Trần Văn B",
                "0987654321",
                "Thôn 1 Bát Tràng",
                "Gia Lâm, Hà Nội",
                20.97,
                105.91,
                true,
                "Chuyên ấm chén",
                null,
                List.of(),
                List.of(),
                status,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Nghệ nhân trạng thái PENDING khi đăng sản phẩm phải ném ForbiddenException")
    void createProductWithPendingArtisanThrowsForbidden() {
        when(artisanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(createProfile(ArtisanStatus.PENDING)));
        Caller caller = Caller.authenticated(USER_ID, "artisan@vcraft.vn", Role.ARTISAN);

        assertThatThrownBy(() -> productService.createProduct(
                caller, "Bình hút lộc", "Men lam cổ", new BigDecimal("1500000"), 10, CATEGORY_ID, List.of(), null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("đang chờ BQL duyệt hoặc đã bị từ chối");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nghệ nhân trạng thái REJECTED khi đăng sản phẩm phải ném ForbiddenException")
    void createProductWithRejectedArtisanThrowsForbidden() {
        when(artisanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(createProfile(ArtisanStatus.REJECTED)));
        Caller caller = Caller.authenticated(USER_ID, "artisan@vcraft.vn", Role.ARTISAN);

        assertThatThrownBy(() -> productService.createProduct(
                caller, "Bình hút lộc", "Men lam cổ", new BigDecimal("1500000"), 10, CATEGORY_ID, List.of(), null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("đang chờ BQL duyệt hoặc đã bị từ chối");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nghệ nhân trạng thái APPROVED đăng sản phẩm thành công")
    void createProductWithApprovedArtisanSucceeds() {
        when(artisanRepository.findByUserId(USER_ID)).thenReturn(Optional.of(createProfile(ArtisanStatus.APPROVED)));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Caller caller = Caller.authenticated(USER_ID, "artisan@vcraft.vn", Role.ARTISAN);

        Product result = productService.createProduct(
                caller, "Bình hút lộc", "Men lam cổ", new BigDecimal("1500000"), 10, CATEGORY_ID, List.of(), null);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Bình hút lộc");
        assertThat(result.artisanId()).isEqualTo(ARTISAN_ID);
        verify(productRepository).save(any(Product.class));
    }
}
