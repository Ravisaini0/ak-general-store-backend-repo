package com.akgeneralstore.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RazorpayOrderResponse {
    private Long orderId;
    private String orderNumber;
    private String razorpayOrderId;
    private long amount;
    private String currency;
    private String keyId;
    private String businessName;
    private String businessLogo;
}
