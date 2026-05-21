package com.akgeneralstore.service.impl;

import com.akgeneralstore.entity.Cart;
import com.akgeneralstore.entity.CartItem;
import com.akgeneralstore.repository.CartItemRepository;
import com.akgeneralstore.repository.CartRepository;
import com.akgeneralstore.service.CartService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public CartServiceImpl(CartRepository cartRepository, CartItemRepository cartItemRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Override
    public List<CartItem> addToCart(Long userId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseGet(CartItem::new);
        item.setCartId(cart.getId());
        item.setProductId(productId);
        item.setQuantity((item.getQuantity() == null ? 0 : item.getQuantity()) + quantity);
        cartItemRepository.save(item);
        return cartItemRepository.findByCartId(cart.getId());
    }

    @Override
    public List<CartItem> getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return cartItemRepository.findByCartId(cart.getId());
    }

    @Override
    public List<CartItem> updateCart(Long userId, Long productId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseGet(CartItem::new);
        item.setCartId(cart.getId());
        item.setProductId(productId);
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return cartItemRepository.findByCartId(cart.getId());
    }

    @Override
    public void removeFromCart(Long userId, Long productId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCartIdAndProductId(cart.getId(), productId);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUserId(userId);
            return cartRepository.save(cart);
        });
    }
}
