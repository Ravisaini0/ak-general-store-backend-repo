package com.akgeneralstore.controller;

import com.akgeneralstore.dto.request.DeliveryCompletionRequest;
import com.akgeneralstore.dto.response.ApiResponse;
import com.akgeneralstore.dto.response.OrderResponse;
import com.akgeneralstore.security.UserPrincipal;
import com.akgeneralstore.service.DeliveryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/orders")
    public ApiResponse<List<OrderResponse>> getOrders(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new ApiResponse<>(true, "Delivery orders fetched", deliveryService.getDeliveryOrders(principal.getId()));
    }

    @PutMapping("/orders/{id}/accept")
    public ApiResponse<OrderResponse> acceptOrder(@PathVariable Long id, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new ApiResponse<>(true, "Order accepted", deliveryService.acceptOrder(id, principal.getId()));
    }

    @PutMapping("/orders/{id}/delivered")
    public ApiResponse<OrderResponse> markDelivered(
            @PathVariable Long id,
            @RequestBody(required = false) DeliveryCompletionRequest request,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new ApiResponse<>(true, "Order delivered", deliveryService.markDelivered(id, principal.getId(), request));
    }

    @PostMapping("/payouts/request")
    public ApiResponse<Void> requestWeeklyWithdrawal(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        deliveryService.requestWeeklyWithdrawal(principal.getId());
        return new ApiResponse<>(true, "Weekly payout request submitted", null);
    }
}
