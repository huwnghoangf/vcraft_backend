package com.vcraft.infrastructure.adapter;

import com.vcraft.domain.model.Cart;
import com.vcraft.domain.model.CartItem;
import com.vcraft.domain.repository.CartRepository;
import com.vcraft.infrastructure.entity.CartEntity;
import com.vcraft.infrastructure.entity.CartItemEntity;
import com.vcraft.infrastructure.repository.CartItemJpaRepository;
import com.vcraft.infrastructure.repository.CartJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CartRepositoryAdapter implements CartRepository {

    private final CartJpaRepository cartJpaRepository;
    private final CartItemJpaRepository cartItemJpaRepository;

    public CartRepositoryAdapter(CartJpaRepository cartJpaRepository, CartItemJpaRepository cartItemJpaRepository) {
        this.cartJpaRepository = cartJpaRepository;
        this.cartItemJpaRepository = cartItemJpaRepository;
    }

    @Override
    public Optional<Cart> findByUserId(UUID userId) {
        return cartJpaRepository.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public Cart save(Cart cart) {
        CartEntity entity = new CartEntity(cart.id(), cart.userId(), cart.createdAt());
        return toDomain(cartJpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void addItem(UUID cartId, UUID productId, int quantity) {
        CartEntity cart = cartJpaRepository.findById(cartId).orElseThrow();
        Optional<CartItemEntity> existing = cartItemJpaRepository.findByCartIdAndProductId(cartId, productId);
        if (existing.isPresent()) {
            CartItemEntity item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemJpaRepository.save(item);
        } else {
            CartItemEntity newItem = new CartItemEntity(UUID.randomUUID(), cart, productId, quantity, Instant.now());
            cartItemJpaRepository.save(newItem);
        }
    }

    @Override
    @Transactional
    public void updateItemQuantity(UUID itemId, int quantity) {
        cartItemJpaRepository.findById(itemId).ifPresent(item -> {
            item.setQuantity(quantity);
            cartItemJpaRepository.save(item);
        });
    }

    @Override
    @Transactional
    public void removeItem(UUID itemId) {
        cartItemJpaRepository.deleteById(itemId);
    }

    @Override
    @Transactional
    public void clearCart(UUID cartId) {
        cartItemJpaRepository.deleteByCartId(cartId);
    }

    private Cart toDomain(CartEntity entity) {
        List<CartItem> items = entity.getItems().stream()
                .map(i -> new CartItem(i.getId(), entity.getId(), i.getProductId(), i.getQuantity(), i.getCreatedAt()))
                .toList();
        return new Cart(entity.getId(), entity.getUserId(), items, entity.getCreatedAt());
    }
}
