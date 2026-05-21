package com.akgeneralstore.dto.request;

import com.akgeneralstore.enums.PaymentMode;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrderRequest {

    private Long userId;
    private Long addressId;
    private String deliveryAddress;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    private String deliveryLocationLabel;
    private String servingStoreName;
    private String couponCode;
    private BigDecimal subtotalAmount;
    private BigDecimal deliveryFee = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private PaymentMode paymentMode = PaymentMode.COD;
    private List<OrderItemRequest> items = new ArrayList<>();

    @Data
    public static class OrderItemRequest {
        private Long productId;
        private Integer quantity;
    }
}
