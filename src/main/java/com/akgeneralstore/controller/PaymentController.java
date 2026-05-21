package com.akgeneralstore.controller;

import com.akgeneralstore.dto.request.PaymentRequest;
import com.akgeneralstore.dto.request.RazorpayOrderRequest;
import com.akgeneralstore.dto.request.RazorpayVerifyRequest;
import com.akgeneralstore.dto.response.ApiResponse;
import com.akgeneralstore.dto.response.OrderResponse;
import com.akgeneralstore.dto.response.PaymentConfigResponse;
import com.akgeneralstore.dto.response.RazorpayOrderResponse;
import com.akgeneralstore.security.UserPrincipal;
import com.akgeneralstore.service.PaymentService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/config")
    public ApiResponse<PaymentConfigResponse> getPaymentConfig() {
        return new ApiResponse<>(true, "Payment config fetched", paymentService.getPaymentConfig());
    }

    @PostMapping
    public ApiResponse<OrderResponse> createPayment(@RequestBody PaymentRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new ApiResponse<>(true, "Payment initiated", paymentService.createPayment(request, principal.getId()));
    }

    @PostMapping("/razorpay/order")
    public ApiResponse<RazorpayOrderResponse> createRazorpayOrder(@RequestBody RazorpayOrderRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new ApiResponse<>(true, "Razorpay order created", paymentService.createRazorpayOrder(request, principal.getId()));
    }

    @PostMapping("/razorpay/verify")
    public ApiResponse<OrderResponse> verifyRazorpayPayment(@RequestBody RazorpayVerifyRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new ApiResponse<>(true, "Payment verified", paymentService.verifyRazorpayPayment(request, principal.getId()));
    }

    @PostMapping("/razorpay/fail/{orderId}")
    public ApiResponse<OrderResponse> markPaymentFailed(@PathVariable Long orderId, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new ApiResponse<>(true, "Payment marked as failed", paymentService.markPaymentFailed(orderId, principal.getId()));
    }
}
