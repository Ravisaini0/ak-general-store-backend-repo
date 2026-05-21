package com.akgeneralstore.dto.request;

import lombok.Data;

@Data
public class RazorpayVerifyRequest {
    private Long orderId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
}
