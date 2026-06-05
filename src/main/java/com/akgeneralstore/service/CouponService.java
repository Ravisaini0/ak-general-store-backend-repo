package com.akgeneralstore.service;

import com.akgeneralstore.dto.response.CouponResponse;
import com.akgeneralstore.dto.response.CouponValidationResponse;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    List<CouponResponse> getPublicCoupons();
    CouponValidationResponse validateCoupon(String code, BigDecimal orderAmount, Long userId);
}
