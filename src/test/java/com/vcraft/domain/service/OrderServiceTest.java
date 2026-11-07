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
import com.vcraft.domain.model.Order;
import com.vcraft.domain.model.OrderItem;
import com.vcraft.domain.model.OrderStatus;
import com.vcraft.domain.model.Role;
import com.vcraft.domain.repository.ArtisanRepository;
import com.vcraft.domain.repository.CartRepository;
import com.vcraft.domain.repository.OrderRepository;
import com.vcraft.domain.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID ARTISAN_USER_ID = UUID.randomUUID();
    private static final UUID OTHER_ARTISAN_USER_ID = UUID.randomUUID();
    private static final UUID ARTISAN_ID = UUID.randomUUID();
    private static final UUID OTHER_ARTISAN_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();

    private OrderRepository orderRepository;
    private CartRepository cartRepository;
    private ProductRepository productRepository;
    private ArtisanRepository artisanRepository;
    private OrderService orderService;

    private Order order;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        cartRepository = mock(CartRepository.class);
        productRepository = mock(ProductRepository.class);
        artisanRepository = mock(ArtisanRepository.class);

        orderService = new OrderService(orderRepository, cartRepository, productRepository, artisanRepository);

        OrderItem item = new OrderItem(
                UUID.randomUUID(),
                ORDER_ID,
                UUID.randomUUID(),
                ARTISAN_ID,
                "Bộ ấm chén Bát Tràng",
                new BigDecimal("500000"),
                2
        );

        order = new Order(
                ORDER_ID,
                BUYER_ID,
                "Hà Nội",
                "COD",
                "Giao giờ hành chính",
                OrderStatus.PENDING,
                new BigDecimal("1000000"),
                null,
                List.of(item),
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Nghệ nhân không sở hữu sản phẩm trong đơn khi sửa trạng thái phải ném ForbiddenException")
    void updateOrderStatusByArtisanNotOwningItemsThrowsForbidden() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        ArtisanProfile otherArtisan = new ArtisanProfile(
                OTHER_ARTISAN_ID, OTHER_ARTISAN_USER_ID, "Xưởng Khác", "Trần C", "0900000000",
                "Bát Tràng", "Gia Lâm", 20.0, 105.0, true, "Xưởng", null, List.of(), List.of(),
                ArtisanStatus.APPROVED, null, Instant.now(), Instant.now()
        );
        when(artisanRepository.findByUserId(OTHER_ARTISAN_USER_ID)).thenReturn(Optional.of(otherArtisan));

        Caller otherArtisanCaller = Caller.authenticated(OTHER_ARTISAN_USER_ID, "other@vcraft.vn", Role.ARTISAN);

        assertThatThrownBy(() -> orderService.updateOrderStatus(
                otherArtisanCaller, ORDER_ID, "CONFIRMED", "Đã nhận đơn"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Đơn hàng không chứa sản phẩm thuộc xưởng của bạn");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nghệ nhân sở hữu sản phẩm trong đơn cập nhật trạng thái thành công")
    void updateOrderStatusByOwningArtisanSucceeds() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        ArtisanProfile owningArtisan = new ArtisanProfile(
                ARTISAN_ID, ARTISAN_USER_ID, "Xưởng Minh Long", "Lê Văn Hùng", "0987654321",
                "Bát Tràng", "Gia Lâm", 20.97, 105.91, true, "Xưởng", null, List.of(), List.of(),
                ArtisanStatus.APPROVED, null, Instant.now(), Instant.now()
        );
        when(artisanRepository.findByUserId(ARTISAN_USER_ID)).thenReturn(Optional.of(owningArtisan));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Caller artisanCaller = Caller.authenticated(ARTISAN_USER_ID, "artisan@vcraft.vn", Role.ARTISAN);

        Order updated = orderService.updateOrderStatus(artisanCaller, ORDER_ID, "CONFIRMED", "Xác nhận");

        assertThat(updated).isNotNull();
        assertThat(updated.status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(any(Order.class));
    }
}
