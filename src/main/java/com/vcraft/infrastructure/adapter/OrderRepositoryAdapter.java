package com.vcraft.infrastructure.adapter;

import com.vcraft.domain.model.Order;
import com.vcraft.domain.model.OrderItem;
import com.vcraft.domain.model.OrderStatus;
import com.vcraft.domain.repository.OrderRepository;
import com.vcraft.infrastructure.entity.OrderEntity;
import com.vcraft.infrastructure.entity.OrderItemEntity;
import com.vcraft.infrastructure.repository.OrderJpaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    public OrderRepositoryAdapter(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = toEntity(order);
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<Order> findByBuyerId(UUID buyerId, OrderStatus status, int page, int limit) {
        var pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit), Sort.by(Sort.Direction.DESC, "createdAt"));
        String statusStr = status != null ? status.name() : null;
        return jpaRepository.findByBuyerIdAndStatus(buyerId, statusStr, pageable).getContent().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByBuyerId(UUID buyerId, OrderStatus status) {
        String statusStr = status != null ? status.name() : null;
        return jpaRepository.countByBuyerIdAndStatus(buyerId, statusStr);
    }

    @Override
    public List<Order> findByArtisanId(UUID artisanId, OrderStatus status, int page, int limit) {
        var pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit), Sort.by(Sort.Direction.DESC, "createdAt"));
        String statusStr = status != null ? status.name() : null;
        return jpaRepository.findByArtisanIdAndStatus(artisanId, statusStr, pageable).getContent().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByArtisanId(UUID artisanId, OrderStatus status) {
        String statusStr = status != null ? status.name() : null;
        return jpaRepository.countByArtisanIdAndStatus(artisanId, statusStr);
    }

    private Order toDomain(OrderEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(i -> new OrderItem(i.getId(), entity.getId(), i.getProductId(), i.getArtisanId(), i.getProductName(), i.getPrice(), i.getQuantity()))
                .toList();

        return new Order(
                entity.getId(),
                entity.getBuyerId(),
                entity.getShippingAddress(),
                entity.getPaymentMethod(),
                entity.getNote(),
                OrderStatus.valueOf(entity.getStatus()),
                entity.getTotalAmount(),
                entity.getCancelReason(),
                items,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private OrderEntity toEntity(Order domain) {
        OrderEntity entity = new OrderEntity(
                domain.id(),
                domain.buyerId(),
                domain.shippingAddress(),
                domain.paymentMethod(),
                domain.note(),
                domain.status().name(),
                domain.totalAmount(),
                domain.cancelReason(),
                domain.createdAt(),
                domain.updatedAt()
        );

        if (domain.items() != null) {
            List<OrderItemEntity> itemEntities = new ArrayList<>();
            for (var item : domain.items()) {
                itemEntities.add(new OrderItemEntity(
                        item.id(),
                        entity,
                        item.productId(),
                        item.artisanId(),
                        item.productName(),
                        item.price(),
                        item.quantity(),
                        entity.getCreatedAt()
                ));
            }
            entity.setItems(itemEntities);
        }

        return entity;
    }
}
