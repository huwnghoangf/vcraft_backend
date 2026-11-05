package com.vcraft.infrastructure.repository;

import com.vcraft.infrastructure.entity.CartItemEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemJpaRepository extends JpaRepository<CartItemEntity, UUID> {
    Optional<CartItemEntity> findByCartIdAndProductId(UUID cartId, UUID productId);

    @Modifying
    @Query("DELETE FROM CartItemEntity c WHERE c.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") UUID cartId);
}
