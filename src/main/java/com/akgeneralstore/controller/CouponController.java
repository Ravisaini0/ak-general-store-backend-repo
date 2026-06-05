package com.akgeneralstore.controller;

import com.akgeneralstore.dto.request.CouponValidationRequest;
import com.akgeneralstore.dto.response.ApiResponse;
import com.akgeneralstore.dto.response.CouponResponse;
import com.akgeneralstore.dto.response.CouponValidationResponse;
import com.akgeneralstore.security.UserPrincipal;
import com.akgeneralstore.service.CouponService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/public")
    public ApiResponse<List<CouponResponse>> getPublicCoupons() {
        return new ApiResponse<>(true, "Live coupons fetched", couponService.getPublicCoupons());
    }

    @PostMapping("/validate")
    public ApiResponse<CouponValidationResponse> validateCoupon(
            @RequestBody CouponValidationRequest request,
            Authentication authentication
    ) {
        Long userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            userId = principal.getId();
        }
        return new ApiResponse<>(
                true,
                "Coupon validated",
                couponService.validateCoupon(request.getCode(), request.getOrderAmount(), userId)
        );
    }
}
