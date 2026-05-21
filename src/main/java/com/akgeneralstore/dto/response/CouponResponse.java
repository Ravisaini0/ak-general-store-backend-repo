package com.akgeneralstore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CouponResponse {
    private Long id;
    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumOrderAmount;
    private Integer maxUsesPerUser;
    private Integer maxTotalUses;
    private LocalDate expiryDate;
    private boolean firstOrderOnly;
    private boolean active;
    private long currentTotalUses;
}
