package com.vcraft.domain.repository;

import com.vcraft.domain.model.Cart;
import com.vcraft.domain.model.CartItem;
import java.util.Optional;
import java.util.UUID;

public interface CartRepository {
    Optional<Cart> findByUserId(UUID userId);
    Cart save(Cart cart);
    void addItem(UUID cartId, UUID productId, int quantity);
    void updateItemQuantity(UUID itemId, int quantity);
    void removeItem(UUID itemId);
    void clearCart(UUID cartId);
}
