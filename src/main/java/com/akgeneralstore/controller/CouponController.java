package com.akgeneralstore.controller;

import com.akgeneralstore.dto.request.CouponValidationRequest;
import com.akgeneralstore.dto.response.ApiResponse;
import com.akgeneralstore.dto.response.CouponValidationResponse;
import com.akgeneralstore.security.UserPrincipal;
import com.akgeneralstore.service.CouponService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/validate")
    public ApiResponse<CouponValidationResponse> validateCoupon(
            @RequestBody CouponValidationRequest request,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new ApiResponse<>(
                true,
                "Coupon validated",
                couponService.validateCoupon(request.getCode(), request.getOrderAmount(), principal.getId())
        );
    }
}
