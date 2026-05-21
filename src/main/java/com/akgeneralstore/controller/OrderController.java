package com.akgeneralstore.controller;

import com.akgeneralstore.dto.request.OrderRequest;
import com.akgeneralstore.dto.response.ApiResponse;
import com.akgeneralstore.dto.response.OrderResponse;
import com.akgeneralstore.security.UserPrincipal;
import com.akgeneralstore.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/place")
    public ApiResponse<OrderResponse> placeOrder(@RequestBody OrderRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        request.setUserId(principal.getId());
        return new ApiResponse<>(true, "Order placed", orderService.placeOrder(request));
    }

    @GetMapping("/my-orders")
    public ApiResponse<List<OrderResponse>> getMyOrders(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new ApiResponse<>(true, "Orders fetched", orderService.getUserOrders(principal.getId()));
    }

    @PutMapping("/{orderNumber}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable String orderNumber, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new ApiResponse<>(true, "Order cancelled", orderService.cancelOrder(principal.getId(), orderNumber));
    }
}
