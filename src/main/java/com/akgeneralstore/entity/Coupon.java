package com.akgeneralstore.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String discountType;

    @Column(nullable = false)
    private BigDecimal discountValue;

    private BigDecimal minimumOrderAmount;
    private Integer maxUsesPerUser;
    private Integer maxTotalUses;
    private LocalDate expiryDate;
    private boolean firstOrderOnly;
    private boolean active = true;
}
