package com.vcraft.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vcraft.domain.exception.NotFoundException;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.Cart;
import com.vcraft.domain.model.CartItem;
import com.vcraft.domain.model.Role;
import com.vcraft.domain.repository.CartRepository;
import com.vcraft.domain.repository.ProductRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CartServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CART_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final UUID OTHER_ITEM_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private CartRepository cartRepository;
    private ProductRepository productRepository;
    private CartService cartService;

    private Caller caller;
    private Cart cart;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        productRepository = mock(ProductRepository.class);
        cartService = new CartService(cartRepository, productRepository);

        caller = Caller.authenticated(USER_ID, "buyer@vcraft.vn", Role.BUYER);
        cartItem = new CartItem(ITEM_ID, CART_ID, PRODUCT_ID, 2, Instant.now());
        cart = new Cart(CART_ID, USER_ID, List.of(cartItem), Instant.now());
    }

    @Test
    @DisplayName("Cập nhật số lượng sản phẩm có trong giỏ hàng thành công")
    void updateItemQuantitySuccess() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        Cart result = cartService.updateItemQuantity(caller, ITEM_ID, 5);

        verify(cartRepository).updateItemQuantity(ITEM_ID, 5);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Cập nhật số lượng sản phẩm không thuộc giỏ hàng của mình phải ném NotFoundException")
    void updateItemQuantityNotInCartThrowsNotFound() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateItemQuantity(caller, OTHER_ITEM_ID, 5))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Mục giỏ hàng không tồn tại trong giỏ của bạn");

        verify(cartRepository, never()).updateItemQuantity(any(), any(int.class));
        verify(cartRepository, never()).removeItem(any());
    }

    @Test
    @DisplayName("Cập nhật số lượng về 0 sẽ xóa sản phẩm khỏi giỏ hàng")
    void updateItemQuantityToZeroRemovesItem() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        cartService.updateItemQuantity(caller, ITEM_ID, 0);

        verify(cartRepository).removeItem(ITEM_ID);
    }

    @Test
    @DisplayName("Xóa sản phẩm có trong giỏ hàng thành công")
    void removeItemSuccess() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        Cart result = cartService.removeItem(caller, ITEM_ID);

        verify(cartRepository).removeItem(ITEM_ID);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Xóa sản phẩm không thuộc giỏ hàng của mình phải ném NotFoundException (ngăn chặn IDOR)")
    void removeItemNotInCartThrowsNotFound() {
        when(cartRepository.findByUserId(USER_ID)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.removeItem(caller, OTHER_ITEM_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Mục giỏ hàng không tồn tại trong giỏ của bạn");

        verify(cartRepository, never()).removeItem(any());
    }
}
