package com.akgeneralstore.dto.request;

import com.akgeneralstore.enums.PaymentMode;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {

    private Long orderId;
    private BigDecimal amount;
    private PaymentMode paymentMode;
    private String referenceId;
}
