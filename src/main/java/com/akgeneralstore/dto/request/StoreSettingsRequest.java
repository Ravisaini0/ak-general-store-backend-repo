package com.akgeneralstore.dto.request;

import lombok.Data;

@Data
public class StoreSettingsRequest {
    private String storeName;
    private String supportPhone;
    private String supportEmail;
    private String freeDeliveryThreshold;
    private String deliveryCharge;
    private String enabledPayments;
    private String serviceRadiusKm;
    private String storeLocations;
    private String upiMerchantName;
    private String upiId;
    private String deliveryBasePayoutAmount;
    private String deliveryAdditionalPayoutAmount;
}
