package com.vcraft.domain.repository;

import com.vcraft.domain.model.Order;
import com.vcraft.domain.model.OrderStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Optional<Order> findById(UUID id);
    Order save(Order order);
    List<Order> findByBuyerId(UUID buyerId, OrderStatus status, int page, int limit);
    long countByBuyerId(UUID buyerId, OrderStatus status);
    List<Order> findByArtisanId(UUID artisanId, OrderStatus status, int page, int limit);
    long countByArtisanId(UUID artisanId, OrderStatus status);
}
