package com.akgeneralstore.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponValidationRequest {
    private String code;
    private BigDecimal orderAmount;
}
