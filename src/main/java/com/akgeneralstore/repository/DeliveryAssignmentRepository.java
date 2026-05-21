package com.akgeneralstore.repository;

import com.akgeneralstore.entity.DeliveryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {
    List<DeliveryAssignment> findByDeliveryBoyId(Long deliveryBoyId);
    Optional<DeliveryAssignment> findByOrderId(Long orderId);
    List<DeliveryAssignment> findByDeliveryBoyIdAndDeliveredAtIsNotNull(Long deliveryBoyId);
}
