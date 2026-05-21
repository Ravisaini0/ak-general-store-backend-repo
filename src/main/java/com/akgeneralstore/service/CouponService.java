package com.akgeneralstore.service;

import com.akgeneralstore.dto.response.CouponValidationResponse;

import java.math.BigDecimal;

public interface CouponService {
    CouponValidationResponse validateCoupon(String code, BigDecimal orderAmount, Long userId);
}
