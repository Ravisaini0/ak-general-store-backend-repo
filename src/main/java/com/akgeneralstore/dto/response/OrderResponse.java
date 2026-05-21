package com.akgeneralstore.dto.response;

import com.akgeneralstore.enums.CollectionMethod;
import com.akgeneralstore.enums.DeliveryBatchStatus;
import com.akgeneralstore.enums.DeliveryBoyStatus;
import com.akgeneralstore.enums.OrderStatus;
import com.akgeneralstore.enums.PaymentMode;
import com.akgeneralstore.enums.PaymentStatus;
import com.akgeneralstore.enums.PayoutStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {

    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private OrderStatus status;
    private Long deliveryBoyId;
    private Long batchId;
    private BigDecimal totalAmount;
    private BigDecimal subtotalAmount;
    private BigDecimal deliveryFee;
    private BigDecimal discountAmount;
    private String couponCode;
    private PaymentMode paymentMode;
    private PaymentStatus paymentStatus;
    private String deliveryAddress;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    private String deliveryLocationLabel;
    private String servingStoreName;
    private Integer batchTotalOrders;
    private BigDecimal batchTotalEarning;
    private DeliveryBatchStatus batchStatus;
    private DeliveryBoyStatus deliveryBoyStatus;
    private LocalDateTime createdAt;
    private String assignedDeliveryName;
    private String assignedDeliveryEmail;
    private String assignedDeliveryPhone;
    private CollectionMethod collectionMethod;
    private BigDecimal collectedAmount;
    private BigDecimal cashCollectedAmount;
    private BigDecimal upiCollectedAmount;
    private Long collectedByDeliveryBoyId;
    private LocalDateTime collectedAt;
    private BigDecimal deliveryEarningAmount;
    private PayoutStatus payoutStatus;
    private LocalDateTime payoutRequestedAt;
    private String payoutReference;
    private LocalDateTime payoutPaidAt;
    private List<String> itemNames;
}
