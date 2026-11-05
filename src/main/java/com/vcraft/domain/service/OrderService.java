package com.vcraft.domain.service;

import com.vcraft.domain.exception.ConflictException;
import com.vcraft.domain.exception.ForbiddenException;
import com.vcraft.domain.exception.InvalidInputException;
import com.vcraft.domain.exception.NotFoundException;
import com.vcraft.domain.model.ArtisanProfile;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.Cart;
import com.vcraft.domain.model.Order;
import com.vcraft.domain.model.OrderItem;
import com.vcraft.domain.model.OrderStatus;
import com.vcraft.domain.model.Product;
import com.vcraft.domain.repository.ArtisanRepository;
import com.vcraft.domain.repository.CartRepository;
import com.vcraft.domain.repository.OrderRepository;
import com.vcraft.domain.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ArtisanRepository artisanRepository;

    public OrderService(OrderRepository orderRepository,
                        CartRepository cartRepository,
                        ProductRepository productRepository,
                        ArtisanRepository artisanRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.artisanRepository = artisanRepository;
    }

    public Order createOrderFromCart(Caller caller, String shippingAddress, String paymentMethod, String note) {
        UUID buyerId = caller.requireUserId();
        Cart cart = cartRepository.findByUserId(buyerId)
                .orElseThrow(() -> new InvalidInputException("Giỏ hàng của bạn đang trống"));

        if (cart.items() == null || cart.items().isEmpty()) {
            throw new InvalidInputException("Giỏ hàng của bạn đang trống");
        }

        UUID orderId = UUID.randomUUID();
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (var item : cart.items()) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new NotFoundException("Sản phẩm không còn tồn tại"));

            BigDecimal lineTotal = product.price().multiply(BigDecimal.valueOf(item.quantity()));
            totalAmount = totalAmount.add(lineTotal);

            orderItems.add(new OrderItem(
                    UUID.randomUUID(),
                    orderId,
                    product.id(),
                    product.artisanId(),
                    product.name(),
                    product.price(),
                    item.quantity()
            ));
        }

        Order order = new Order(
                orderId,
                buyerId,
                shippingAddress,
                paymentMethod != null ? paymentMethod : "COD",
                note,
                OrderStatus.PENDING,
                totalAmount,
                null,
                orderItems,
                Instant.now(),
                Instant.now()
        );

        Order saved = orderRepository.save(order);
        cartRepository.clearCart(cart.id());
        return saved;
    }

    public List<Order> getBuyerOrders(Caller caller, String status, int page, int limit) {
        UUID buyerId = caller.requireUserId();
        OrderStatus orderStatus = parseStatus(status);
        return orderRepository.findByBuyerId(buyerId, orderStatus, page, limit);
    }

    public long countBuyerOrders(Caller caller, String status) {
        UUID buyerId = caller.requireUserId();
        OrderStatus orderStatus = parseStatus(status);
        return orderRepository.countByBuyerId(buyerId, orderStatus);
    }

    public Order getOrderDetail(Caller caller, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));

        // Check permission: buyer, or artisan owning one of the items, or admin
        if (!caller.isAdmin() && !order.buyerId().equals(caller.requireUserId())) {
            UUID userId = caller.requireUserId();
            ArtisanProfile artisan = artisanRepository.findByUserId(userId).orElse(null);
            boolean ownsItem = artisan != null && order.items().stream()
                    .anyMatch(i -> i.artisanId().equals(artisan.id()));
            if (!ownsItem) {
                throw new ForbiddenException("Bạn không có quyền xem đơn hàng này");
            }
        }

        return order;
    }

    public Order cancelOrder(Caller caller, UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));

        if (!order.buyerId().equals(caller.requireUserId())) {
            throw new ForbiddenException("Chỉ người đặt hàng mới có thể hủy đơn này");
        }

        if (order.status() != OrderStatus.PENDING) {
            throw new ConflictException("Đơn hàng đã được xác nhận hoặc đang vận chuyển, không thể hủy");
        }

        Order updated = new Order(
                order.id(), order.buyerId(), order.shippingAddress(), order.paymentMethod(),
                order.note(), OrderStatus.CANCELLED, order.totalAmount(), reason,
                order.items(), order.createdAt(), Instant.now()
        );
        return orderRepository.save(updated);
    }

    public List<Order> getArtisanOrders(Caller caller, String status, int page, int limit) {
        UUID userId = caller.requireUserId();
        ArtisanProfile artisan = artisanRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Chưa có hồ sơ nghệ nhân"));

        OrderStatus orderStatus = parseStatus(status);
        return orderRepository.findByArtisanId(artisan.id(), orderStatus, page, limit);
    }

    public long countArtisanOrders(Caller caller, String status) {
        UUID userId = caller.requireUserId();
        ArtisanProfile artisan = artisanRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Chưa có hồ sơ nghệ nhân"));

        OrderStatus orderStatus = parseStatus(status);
        return orderRepository.countByArtisanId(artisan.id(), orderStatus);
    }

    public Order updateOrderStatus(Caller caller, UUID orderId, String newStatus, String note) {
        caller.requireArtisan();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));

        OrderStatus status = parseStatus(newStatus);
        if (status == null) {
            throw new InvalidInputException("Trạng thái đơn hàng không hợp lệ");
        }

        Order updated = new Order(
                order.id(), order.buyerId(), order.shippingAddress(), order.paymentMethod(),
                note != null ? note : order.note(), status, order.totalAmount(),
                order.cancelReason(), order.items(), order.createdAt(), Instant.now()
        );
        return orderRepository.save(updated);
    }

    private OrderStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}
