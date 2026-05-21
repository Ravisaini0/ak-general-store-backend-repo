package com.akgeneralstore.entity;

import com.akgeneralstore.enums.OrderStatus;
import com.akgeneralstore.enums.PaymentMode;
import com.akgeneralstore.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "orders_tbl")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @Column(nullable = false)
    private Long userId;

    private Long deliveryBoyId;

    private Long batchId;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    private BigDecimal subtotalAmount;

    private BigDecimal deliveryFee;

    private BigDecimal discountAmount;

    private String couponCode;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String deliveryAddress;

    private Double deliveryLatitude;

    private Double deliveryLongitude;

    private String deliveryLocationLabel;

    private String servingStoreName;

    private BigDecimal deliveryEarning;

    private LocalDateTime createdAt;
}
