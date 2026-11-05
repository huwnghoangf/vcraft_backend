package com.vcraft.api.cart;

import com.vcraft.api.CallerResolver;
import com.vcraft.api.cart.dto.AddToCartRequest;
import com.vcraft.api.cart.dto.UpdateCartItemRequest;
import com.vcraft.domain.model.Caller;
import com.vcraft.domain.model.Cart;
import com.vcraft.domain.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "11. Cart", description = "Các API giỏ hàng người dùng")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Xem giỏ hàng hiện tại của người dùng")
    public ResponseEntity<Cart> getCart(Authentication authentication) {
        Caller caller = CallerResolver.resolve(authentication);
        return ResponseEntity.ok(cartService.getOrCreateCart(caller));
    }

    @PostMapping("/items")
    @Operation(summary = "Thêm sản phẩm vào giỏ hàng")
    public ResponseEntity<Cart> addItem(Authentication authentication,
                                        @Valid @RequestBody AddToCartRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        return ResponseEntity.ok(cartService.addItem(caller, request.productId(), request.quantity()));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Cập nhật số lượng một sản phẩm trong giỏ")
    public ResponseEntity<Cart> updateItem(Authentication authentication,
                                           @PathVariable UUID itemId,
                                           @Valid @RequestBody UpdateCartItemRequest request) {
        Caller caller = CallerResolver.resolve(authentication);
        return ResponseEntity.ok(cartService.updateItemQuantity(caller, itemId, request.quantity()));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Xóa một sản phẩm khỏi giỏ hàng")
    public ResponseEntity<Cart> removeItem(Authentication authentication,
                                           @PathVariable UUID itemId) {
        Caller caller = CallerResolver.resolve(authentication);
        return ResponseEntity.ok(cartService.removeItem(caller, itemId));
    }

    @DeleteMapping
    @Operation(summary = "Xóa toàn bộ giỏ hàng")
    public ResponseEntity<Map<String, String>> clearCart(Authentication authentication) {
        Caller caller = CallerResolver.resolve(authentication);
        cartService.clearCart(caller);
        return ResponseEntity.ok(Map.of("message", "Cart cleared successfully"));
    }
}
