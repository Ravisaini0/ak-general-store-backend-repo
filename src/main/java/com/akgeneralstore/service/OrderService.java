package com.akgeneralstore.service;

import com.akgeneralstore.dto.request.OrderRequest;
import com.akgeneralstore.dto.response.OrderResponse;
import com.akgeneralstore.enums.OrderStatus;

import java.util.List;

public interface OrderService {
    OrderResponse placeOrder(OrderRequest request);
    List<OrderResponse> getUserOrders(Long userId);
    List<OrderResponse> getAllOrders();
    OrderResponse updateOrderStatus(Long orderId, OrderStatus status);
    OrderResponse cancelOrder(Long userId, String orderNumber);
}
