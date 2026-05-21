package com.akgeneralstore.entity;

import com.akgeneralstore.enums.DeliveryBatchStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "delivery_batches")
public class DeliveryBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long deliveryBoyId;

    private Integer totalOrders;
    private BigDecimal totalEarning;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryBatchStatus status = DeliveryBatchStatus.ACTIVE;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
