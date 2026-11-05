package com.vcraft.domain.service;

import com.vcraft.domain.exception.NotFoundException;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.Cart;
import com.vcraft.domain.model.Product;
import com.vcraft.domain.repository.CartRepository;
import com.vcraft.domain.repository.ProductRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    public Cart getOrCreateCart(Caller caller) {
        UUID userId = caller.requireUserId();
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart(UUID.randomUUID(), userId, List.of(), Instant.now());
                    return cartRepository.save(newCart);
                });
    }

    public Cart addItem(Caller caller, UUID productId, int quantity) {
        Cart cart = getOrCreateCart(caller);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại"));

        cartRepository.addItem(cart.id(), product.id(), quantity > 0 ? quantity : 1);
        return getOrCreateCart(caller);
    }

    public Cart updateItemQuantity(Caller caller, UUID itemId, int quantity) {
        Cart cart = getOrCreateCart(caller);
        if (quantity <= 0) {
            cartRepository.removeItem(itemId);
        } else {
            cartRepository.updateItemQuantity(itemId, quantity);
        }
        return getOrCreateCart(caller);
    }

    public Cart removeItem(Caller caller, UUID itemId) {
        cartRepository.removeItem(itemId);
        return getOrCreateCart(caller);
    }

    public void clearCart(Caller caller) {
        Cart cart = getOrCreateCart(caller);
        cartRepository.clearCart(cart.id());
    }
}
