package com.akgeneralstore.entity;

import com.akgeneralstore.enums.CollectionMethod;
import com.akgeneralstore.enums.PaymentMode;
import com.akgeneralstore.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private BigDecimal amount;

    private String providerOrderId;
    private String providerPaymentId;

    @Column(columnDefinition = "TEXT")
    private String providerSignature;

    @Enumerated(EnumType.STRING)
    private PaymentMode mode;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private CollectionMethod collectionMethod;

    private Long collectedByDeliveryBoyId;

    private LocalDateTime collectedAt;

    private LocalDateTime createdAt;
}
