package com.akgeneralstore.service;

import com.akgeneralstore.dto.request.PaymentRequest;
import com.akgeneralstore.dto.request.RazorpayOrderRequest;
import com.akgeneralstore.dto.request.RazorpayVerifyRequest;
import com.akgeneralstore.dto.response.OrderResponse;
import com.akgeneralstore.dto.response.PaymentConfigResponse;
import com.akgeneralstore.dto.response.RazorpayOrderResponse;

public interface PaymentService {
    OrderResponse createPayment(PaymentRequest request, Long userId);
    PaymentConfigResponse getPaymentConfig();
    RazorpayOrderResponse createRazorpayOrder(RazorpayOrderRequest request, Long userId);
    OrderResponse verifyRazorpayPayment(RazorpayVerifyRequest request, Long userId);
    OrderResponse markPaymentFailed(Long orderId, Long userId);
}
