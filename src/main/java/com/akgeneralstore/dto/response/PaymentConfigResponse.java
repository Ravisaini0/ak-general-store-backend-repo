package com.akgeneralstore.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentConfigResponse {
    private boolean razorpayEnabled;
    private String razorpayKeyId;
    private String businessName;
    private String businessLogo;
    private String upiMerchantName;
    private String upiId;
    private String deliveryBasePayoutAmount;
    private String deliveryAdditionalPayoutAmount;
}
