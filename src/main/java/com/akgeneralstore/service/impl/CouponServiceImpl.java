package com.akgeneralstore.service.impl;

import com.akgeneralstore.dto.response.CouponValidationResponse;
import com.akgeneralstore.entity.Coupon;
import com.akgeneralstore.exception.BadRequestException;
import com.akgeneralstore.repository.CouponRepository;
import com.akgeneralstore.repository.OrderRepository;
import com.akgeneralstore.service.CouponService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;

    public CouponServiceImpl(CouponRepository couponRepository, OrderRepository orderRepository) {
        this.couponRepository = couponRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public CouponValidationResponse validateCoupon(String code, BigDecimal orderAmount, Long userId) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Please enter a coupon code.");
        }

        BigDecimal safeOrderAmount = orderAmount == null ? BigDecimal.ZERO : orderAmount.max(BigDecimal.ZERO);
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BadRequestException("This coupon code is not valid."));

        if (!coupon.isActive()) {
            throw new BadRequestException("This coupon is currently inactive.");
        }

        BigDecimal minimumOrderAmount = coupon.getMinimumOrderAmount() == null
                ? BigDecimal.ZERO
                : coupon.getMinimumOrderAmount();
        long currentTotalUses = orderRepository.countByCouponCodeIgnoreCase(coupon.getCode());
        long currentUserUses = userId == null ? 0 : orderRepository.countByUserIdAndCouponCodeIgnoreCase(userId, coupon.getCode());

        if (safeOrderAmount.compareTo(minimumOrderAmount) < 0) {
            throw new BadRequestException("This coupon requires a minimum cart value of Rs" + minimumOrderAmount + ".");
        }

        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("This coupon has expired.");
        }

        if (coupon.getMaxTotalUses() != null && coupon.getMaxTotalUses() > 0 && currentTotalUses >= coupon.getMaxTotalUses()) {
            throw new BadRequestException("This coupon has reached its total usage limit.");
        }

        if (coupon.getMaxUsesPerUser() != null && coupon.getMaxUsesPerUser() > 0 && currentUserUses >= coupon.getMaxUsesPerUser()) {
            throw new BadRequestException("You have already used this coupon the maximum allowed number of times.");
        }

        if (coupon.isFirstOrderOnly() && userId != null && orderRepository.countByUserId(userId) > 0) {
            throw new BadRequestException("This coupon is only available on the first successful order.");
        }

        BigDecimal discountAmount = calculateDiscountAmount(coupon, safeOrderAmount);
        BigDecimal finalOrderAmount = safeOrderAmount.subtract(discountAmount).max(BigDecimal.ZERO);

        return CouponValidationResponse.builder()
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minimumOrderAmount(minimumOrderAmount)
                .maxUsesPerUser(coupon.getMaxUsesPerUser())
                .maxTotalUses(coupon.getMaxTotalUses())
                .expiryDate(coupon.getExpiryDate())
                .firstOrderOnly(coupon.isFirstOrderOnly())
                .currentUserUses(currentUserUses)
                .currentTotalUses(currentTotalUses)
                .discountAmount(discountAmount)
                .finalOrderAmount(finalOrderAmount)
                .message("Coupon applied successfully.")
                .build();
    }

    private BigDecimal calculateDiscountAmount(Coupon coupon, BigDecimal orderAmount) {
        if ("PERCENT".equalsIgnoreCase(coupon.getDiscountType())) {
            return orderAmount
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    .min(orderAmount);
        }

        return coupon.getDiscountValue().min(orderAmount).setScale(2, RoundingMode.HALF_UP);
    }
}
