package com.akgeneralstore.repository;

import com.akgeneralstore.entity.DeliveryBatch;
import com.akgeneralstore.enums.DeliveryBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryBatchRepository extends JpaRepository<DeliveryBatch, Long> {
    Optional<DeliveryBatch> findFirstByDeliveryBoyIdAndStatusOrderByIdDesc(Long deliveryBoyId, DeliveryBatchStatus status);
    List<DeliveryBatch> findByDeliveryBoyIdOrderByIdDesc(Long deliveryBoyId);
}
