package com.akgeneralstore.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CouponRequest {
    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumOrderAmount;
    private Integer maxUsesPerUser;
    private Integer maxTotalUses;
    private LocalDate expiryDate;
    private boolean firstOrderOnly;
    private boolean active = true;
}
