package com.akgeneralstore.controller;

import com.akgeneralstore.dto.response.ApiResponse;
import com.akgeneralstore.entity.CartItem;
import com.akgeneralstore.service.CartService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public ApiResponse<List<CartItem>> addToCart(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(String.valueOf(body.getOrDefault("userId", 1)));
        Long productId = Long.valueOf(String.valueOf(body.get("productId")));
        Integer quantity = Integer.valueOf(String.valueOf(body.getOrDefault("quantity", 1)));
        return new ApiResponse<>(true, "Cart updated", cartService.addToCart(userId, productId, quantity));
    }

    @GetMapping
    public ApiResponse<List<CartItem>> getCart(@RequestParam(defaultValue = "1") Long userId) {
        return new ApiResponse<>(true, "Cart fetched", cartService.getCart(userId));
    }

    @PutMapping("/update")
    public ApiResponse<List<CartItem>> updateCart(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(String.valueOf(body.getOrDefault("userId", 1)));
        Long productId = Long.valueOf(String.valueOf(body.get("productId")));
        Integer quantity = Integer.valueOf(String.valueOf(body.getOrDefault("quantity", 1)));
        return new ApiResponse<>(true, "Cart updated", cartService.updateCart(userId, productId, quantity));
    }

    @DeleteMapping("/remove/{id}")
    public ApiResponse<Void> removeFromCart(@PathVariable("id") Long productId, @RequestParam(defaultValue = "1") Long userId) {
        cartService.removeFromCart(userId, productId);
        return new ApiResponse<>(true, "Item removed", null);
    }
}
