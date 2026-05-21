package com.akgeneralstore.entity;

import com.akgeneralstore.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "delivery_assignments")
public class DeliveryAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private Long deliveryBoyId;

    private LocalDateTime acceptedAt;
    private LocalDateTime deliveredAt;
    private Double currentLat;
    private Double currentLng;
    private BigDecimal collectedAmount;
    private BigDecimal cashCollectedAmount;
    private BigDecimal upiCollectedAmount;
    private BigDecimal earningAmount;

    @Enumerated(EnumType.STRING)
    private PayoutStatus payoutStatus = PayoutStatus.PENDING;

    private LocalDateTime payoutRequestedAt;
    private String payoutReference;
    private LocalDateTime payoutPaidAt;
}
