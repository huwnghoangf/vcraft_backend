package com.vcraft.api.order;

import com.vcraft.api.CallerResolver;
import com.vcraft.api.dto.PageResponse;
import com.vcraft.api.order.dto.CancelOrderRequest;
import com.vcraft.api.order.dto.CreateOrderRequest;
import com.vcraft.api.order.dto.UpdateOrderStatusRequest;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.Order;
import com.vcraft.domain.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "11. Orders", description = "Các API đặt hàng và quản lý vòng đời đơn hàng mua lẻ")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    @Operation(summary = "Buyer đặt hàng từ giỏ hàng hiện tại")
    public ResponseEntity<Order> createOrder(Authentication authentication,
                                             @Valid @RequestBody CreateOrderRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        Order order = orderService.createOrderFromCart(caller, request.shippingAddress(), request.paymentMethod(), request.note());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/orders")
    @Operation(summary = "Lịch sử đơn hàng của Buyer")
    public ResponseEntity<PageResponse<Order>> getMyOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status
    ) {
        Caller caller = CallerResolver.resolve(authentication);
        List<Order> data = orderService.getBuyerOrders(caller, status, page, limit);
        long total = orderService.countBuyerOrders(caller, status);
        return ResponseEntity.ok(PageResponse.of(data, page, limit, total));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Chi tiết một đơn hàng")
    public ResponseEntity<Order> getOrderDetail(Authentication authentication,
                                                @PathVariable UUID id) {
        Caller caller = CallerResolver.resolve(authentication);
        return ResponseEntity.ok(orderService.getOrderDetail(caller, id));
    }

    @PatchMapping("/orders/{id}/cancel")
    @Operation(summary = "Buyer hủy đơn hàng (khi đơn còn ở trạng thái Pending)")
    public ResponseEntity<Order> cancelOrder(Authentication authentication,
                                             @PathVariable UUID id,
                                             @RequestBody(required = false) CancelOrderRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        String reason = request != null ? request.reason() : "Buyer cancelled";
        return ResponseEntity.ok(orderService.cancelOrder(caller, id, reason));
    }

    @GetMapping("/artisans/me/orders")
    @Operation(summary = "Nghệ nhân xem danh sách đơn hàng mà xưởng nhận được")
    public ResponseEntity<PageResponse<Order>> getArtisanOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status
    ) {
        Caller caller = CallerResolver.resolve(authentication);
        List<Order> data = orderService.getArtisanOrders(caller, status, page, limit);
        long total = orderService.countArtisanOrders(caller, status);
        return ResponseEntity.ok(PageResponse.of(data, page, limit, total));
    }

    @PatchMapping("/orders/{id}/status")
    @Operation(summary = "Nghệ nhân cập nhật trạng thái đơn hàng (Confirmed | Shipping | Delivered)")
    public ResponseEntity<Order> updateOrderStatus(Authentication authentication,
                                                   @PathVariable UUID id,
                                                   @Valid @RequestBody UpdateOrderStatusRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        return ResponseEntity.ok(orderService.updateOrderStatus(caller, id, request.status(), request.note()));
    }
}
