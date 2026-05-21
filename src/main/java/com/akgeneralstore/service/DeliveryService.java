package com.akgeneralstore.service;

import com.akgeneralstore.dto.request.DeliveryCompletionRequest;
import com.akgeneralstore.dto.response.OrderResponse;

import java.util.List;

public interface DeliveryService {
    List<OrderResponse> getDeliveryOrders(Long deliveryBoyId);
    OrderResponse assignOrder(Long orderId, Long deliveryBoyId);
    OrderResponse acceptOrder(Long orderId, Long deliveryBoyId);
    OrderResponse markDelivered(Long orderId, Long deliveryBoyId, DeliveryCompletionRequest request);
    void requestWeeklyWithdrawal(Long deliveryBoyId);
    void validateDeliveryPartnerForAssignment(Long deliveryBoyId);
}
