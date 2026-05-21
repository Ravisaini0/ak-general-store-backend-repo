package com.akgeneralstore.service;

import com.akgeneralstore.entity.CartItem;

import java.util.List;

public interface CartService {
    List<CartItem> addToCart(Long userId, Long productId, Integer quantity);
    List<CartItem> getCart(Long userId);
    List<CartItem> updateCart(Long userId, Long productId, Integer quantity);
    void removeFromCart(Long userId, Long productId);
}
