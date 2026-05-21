package com.akgeneralstore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CouponValidationResponse {
    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumOrderAmount;
    private Integer maxUsesPerUser;
    private Integer maxTotalUses;
    private LocalDate expiryDate;
    private boolean firstOrderOnly;
    private long currentUserUses;
    private long currentTotalUses;
    private BigDecimal discountAmount;
    private BigDecimal finalOrderAmount;
    private String message;
}
