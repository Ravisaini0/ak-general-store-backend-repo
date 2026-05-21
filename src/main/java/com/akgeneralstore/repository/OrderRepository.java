package com.akgeneralstore.repository;

import com.akgeneralstore.entity.Order;
import com.akgeneralstore.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    List<Order> findByBatchId(Long batchId);
    List<Order> findByDeliveryBoyId(Long deliveryBoyId);
    Optional<Order> findByOrderNumber(String orderNumber);
    long countByUserId(Long userId);
    long countByCouponCodeIgnoreCase(String couponCode);
    long countByUserIdAndCouponCodeIgnoreCase(Long userId, String couponCode);
    List<Order> findTop5ByOrderByCreatedAtDesc();
    List<Order> findByStatusIn(List<OrderStatus> statuses);
}
