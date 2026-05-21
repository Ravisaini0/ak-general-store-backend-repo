package com.akgeneralstore.service.impl;

import com.akgeneralstore.dto.request.DeliveryCompletionRequest;
import com.akgeneralstore.dto.response.OrderResponse;
import com.akgeneralstore.entity.DeliveryAssignment;
import com.akgeneralstore.entity.DeliveryBatch;
import com.akgeneralstore.entity.Order;
import com.akgeneralstore.entity.Payment;
import com.akgeneralstore.entity.StoreSetting;
import com.akgeneralstore.entity.User;
import com.akgeneralstore.enums.CollectionMethod;
import com.akgeneralstore.enums.DeliveryBatchStatus;
import com.akgeneralstore.enums.DeliveryBoyStatus;
import com.akgeneralstore.enums.OrderStatus;
import com.akgeneralstore.enums.PaymentMode;
import com.akgeneralstore.enums.PaymentStatus;
import com.akgeneralstore.enums.PayoutStatus;
import com.akgeneralstore.enums.UserRole;
import com.akgeneralstore.exception.BadRequestException;
import com.akgeneralstore.exception.ResourceNotFoundException;
import com.akgeneralstore.exception.UnauthorizedException;
import com.akgeneralstore.repository.DeliveryAssignmentRepository;
import com.akgeneralstore.repository.DeliveryBatchRepository;
import com.akgeneralstore.repository.OrderRepository;
import com.akgeneralstore.repository.PaymentRepository;
import com.akgeneralstore.repository.StoreSettingRepository;
import com.akgeneralstore.repository.UserRepository;
import com.akgeneralstore.service.DeliveryService;
import com.akgeneralstore.service.OrderService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class DeliveryServiceImpl implements DeliveryService {

    private static final int WITHDRAWAL_WINDOW_DAYS = 7;
    private static final double NEARBY_BATCH_RADIUS_KM = 3D;

    private final OrderRepository orderRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final DeliveryBatchRepository deliveryBatchRepository;
    private final PaymentRepository paymentRepository;
    private final StoreSettingRepository storeSettingRepository;
    private final OrderService orderService;
    private final UserRepository userRepository;

    public DeliveryServiceImpl(
            OrderRepository orderRepository,
            DeliveryAssignmentRepository deliveryAssignmentRepository,
            DeliveryBatchRepository deliveryBatchRepository,
            PaymentRepository paymentRepository,
            StoreSettingRepository storeSettingRepository,
            OrderService orderService,
            UserRepository userRepository
    ) {
        this.orderRepository = orderRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.deliveryBatchRepository = deliveryBatchRepository;
        this.paymentRepository = paymentRepository;
        this.storeSettingRepository = storeSettingRepository;
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @Override
    public List<OrderResponse> getDeliveryOrders(Long deliveryBoyId) {
        User deliveryPartner = validateAndGetDeliveryPartner(deliveryBoyId);
        return orderRepository.findByDeliveryBoyId(deliveryBoyId).stream()
                .filter(order -> List.of(
                        OrderStatus.ACCEPTED,
                        OrderStatus.PICKED_UP,
                        OrderStatus.OUT_FOR_DELIVERY,
                        OrderStatus.DELIVERED
                ).contains(order.getStatus()))
                .map(order -> orderService.getAllOrders().stream()
                        .filter(response -> response.getOrderId().equals(order.getId()))
                        .findFirst()
                        .orElse(null))
                .filter(item -> item != null)
                .sorted(Comparator.comparing(OrderResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public OrderResponse assignOrder(Long orderId, Long deliveryBoyId) {
        User deliveryPartner = validateAndGetDeliveryPartner(deliveryBoyId);
        ensureAvailableForPickup(deliveryPartner);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!List.of(OrderStatus.CONFIRMED, OrderStatus.PENDING).contains(order.getStatus())) {
            throw new BadRequestException("Only confirmed orders can be assigned to a delivery batch.");
        }

        if (order.getDeliveryBoyId() != null && !order.getDeliveryBoyId().equals(deliveryBoyId)) {
            throw new BadRequestException("This order is already assigned to another delivery partner.");
        }

        DeliveryBatch batch = getOrCreatePickupBatch(deliveryPartner, order);
        int existingBatchOrders = orderRepository.findByBatchId(batch.getId()).size();
        BigDecimal earning = existingBatchOrders == 0
                ? readDecimalSetting("delivery_base_payout_amount", BigDecimal.valueOf(20))
                : readDecimalSetting("delivery_additional_payout_amount", BigDecimal.valueOf(10));

        order.setDeliveryBoyId(deliveryBoyId);
        order.setBatchId(batch.getId());
        order.setDeliveryEarning(earning);
        order.setStatus(OrderStatus.ACCEPTED);
        orderRepository.save(order);

        DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderId(orderId)
                .orElseGet(DeliveryAssignment::new);
        assignment.setOrderId(orderId);
        assignment.setDeliveryBoyId(deliveryBoyId);
        assignment.setEarningAmount(earning);
        assignment.setPayoutStatus(PayoutStatus.PENDING);
        deliveryAssignmentRepository.save(assignment);

        recalculateBatchTotals(batch.getId());
        return orderService.getAllOrders().stream()
                .filter(response -> response.getOrderId().equals(order.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Assigned order could not be mapped."));
    }

    @Override
    public OrderResponse acceptOrder(Long orderId, Long deliveryBoyId) {
        User deliveryPartner = validateAndGetDeliveryPartner(deliveryBoyId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!deliveryBoyIdEquals(order, deliveryBoyId)) {
            throw new UnauthorizedException("This order is assigned to another delivery partner.");
        }
        if (order.getBatchId() == null) {
            throw new BadRequestException("This order is not linked to a pickup batch yet.");
        }
        if (deliveryPartner.getDeliveryStatus() == DeliveryBoyStatus.OFFLINE) {
            throw new BadRequestException("Go online before starting a delivery batch.");
        }
        if (deliveryPartner.getDeliveryStatus() == DeliveryBoyStatus.ON_DELIVERY
                && order.getStatus() != OrderStatus.PICKED_UP
                && order.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            throw new BadRequestException("You are already on a delivery route. Finish the current batch before starting another one.");
        }

        DeliveryBatch batch = deliveryBatchRepository.findById(order.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery batch not found"));
        List<Order> batchOrders = orderRepository.findByBatchId(batch.getId());

        if (batch.getStartedAt() == null) {
            batch.setStartedAt(LocalDateTime.now());
            deliveryBatchRepository.save(batch);
            deliveryPartner.setDeliveryStatus(DeliveryBoyStatus.ON_DELIVERY);
            userRepository.save(deliveryPartner);

            for (Order batchOrder : batchOrders) {
                if (batchOrder.getStatus() == OrderStatus.ACCEPTED) {
                    batchOrder.setStatus(OrderStatus.PICKED_UP);
                    orderRepository.save(batchOrder);
                }
            }
        }

        if (order.getStatus() == OrderStatus.ACCEPTED) {
            order.setStatus(OrderStatus.PICKED_UP);
        }
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        orderRepository.save(order);

        DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderId(orderId)
                .orElseGet(DeliveryAssignment::new);
        assignment.setOrderId(orderId);
        assignment.setDeliveryBoyId(deliveryBoyId);
        if (assignment.getAcceptedAt() == null) {
            assignment.setAcceptedAt(LocalDateTime.now());
        }
        if (assignment.getEarningAmount() == null) {
            assignment.setEarningAmount(order.getDeliveryEarning());
        }
        if (assignment.getPayoutStatus() == null) {
            assignment.setPayoutStatus(PayoutStatus.PENDING);
        }
        deliveryAssignmentRepository.save(assignment);

        return orderService.getAllOrders().stream()
                .filter(response -> response.getOrderId().equals(order.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Picked order could not be mapped."));
    }

    @Override
    public OrderResponse markDelivered(Long orderId, Long deliveryBoyId, DeliveryCompletionRequest request) {
        User deliveryPartner = validateAndGetDeliveryPartner(deliveryBoyId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!deliveryBoyIdEquals(order, deliveryBoyId)) {
            throw new UnauthorizedException("This order is assigned to another delivery partner.");
        }

        DeliveryAssignment assignment = deliveryAssignmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery assignment not found"));
        Payment payment = paymentRepository.findByOrderId(orderId).orElseGet(Payment::new);

        if (order.getPaymentMode() == PaymentMode.COD) {
            if (request == null || request.getCollectionMethod() == null) {
                throw new BadRequestException("Select cash or UPI collection before completing COD delivery.");
            }

            if (request.getCollectionMethod() == CollectionMethod.UPI &&
                    (request.getReferenceId() == null || request.getReferenceId().isBlank())) {
                throw new BadRequestException("Enter the UPI reference before completing this delivery.");
            }

            payment.setCollectionMethod(request.getCollectionMethod());
            payment.setCollectedByDeliveryBoyId(deliveryBoyId);
            payment.setCollectedAt(LocalDateTime.now());
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setMode(PaymentMode.COD);
            payment.setAmount(order.getTotalAmount());
            payment.setTransactionId(
                    request.getCollectionMethod() == CollectionMethod.CASH
                            ? "COD-CASH-" + order.getOrderNumber()
                            : request.getReferenceId().trim()
            );
            payment.setOrderId(orderId);
            if (payment.getCreatedAt() == null) {
                payment.setCreatedAt(LocalDateTime.now());
            }
            paymentRepository.save(payment);
            order.setPaymentStatus(PaymentStatus.SUCCESS);

            assignment.setCollectedAmount(order.getTotalAmount());
            assignment.setCashCollectedAmount(
                    request.getCollectionMethod() == CollectionMethod.CASH ? order.getTotalAmount() : BigDecimal.ZERO
            );
            assignment.setUpiCollectedAmount(
                    request.getCollectionMethod() == CollectionMethod.UPI ? order.getTotalAmount() : BigDecimal.ZERO
            );
        } else {
            payment.setCollectionMethod(CollectionMethod.ONLINE);
            if (payment.getCreatedAt() == null) {
                payment.setCreatedAt(LocalDateTime.now());
            }
            if (payment.getStatus() == null || payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.SUCCESS);
            }
            payment.setOrderId(orderId);
            payment.setAmount(order.getTotalAmount());
            payment.setMode(order.getPaymentMode());
            if (payment.getTransactionId() == null || payment.getTransactionId().isBlank()) {
                payment.setTransactionId("ONLINE-" + order.getOrderNumber());
            }
            paymentRepository.save(payment);
            order.setPaymentStatus(PaymentStatus.SUCCESS);
            assignment.setCollectedAmount(BigDecimal.ZERO);
            assignment.setCashCollectedAmount(BigDecimal.ZERO);
            assignment.setUpiCollectedAmount(BigDecimal.ZERO);
        }

        assignment.setDeliveredAt(LocalDateTime.now());
        if (assignment.getEarningAmount() == null) {
            assignment.setEarningAmount(order.getDeliveryEarning());
        }
        if (assignment.getPayoutStatus() == null) {
            assignment.setPayoutStatus(PayoutStatus.PENDING);
        }
        deliveryAssignmentRepository.save(assignment);

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        if (order.getBatchId() != null) {
            List<Order> remainingOrders = orderRepository.findByBatchId(order.getBatchId()).stream()
                    .filter(item -> item.getStatus() != OrderStatus.DELIVERED && item.getStatus() != OrderStatus.CANCELLED)
                    .toList();

            if (remainingOrders.isEmpty()) {
                deliveryPartner.setDeliveryStatus(DeliveryBoyStatus.AT_SHOP);
                userRepository.save(deliveryPartner);
                deliveryBatchRepository.findById(order.getBatchId()).ifPresent(batch -> {
                    batch.setCompletedAt(LocalDateTime.now());
                    batch.setStatus(DeliveryBatchStatus.COMPLETED);
                    deliveryBatchRepository.save(batch);
                });
            } else {
                Order nextRouteOrder = remainingOrders.stream()
                        .filter(item -> item.getStatus() == OrderStatus.PICKED_UP)
                        .findFirst()
                        .orElse(null);
                if (nextRouteOrder != null) {
                    nextRouteOrder.setStatus(OrderStatus.OUT_FOR_DELIVERY);
                    orderRepository.save(nextRouteOrder);
                }
            }
        }

        return orderService.getAllOrders().stream()
                .filter(response -> response.getOrderId().equals(order.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Delivered order could not be mapped."));
    }

    @Override
    public void requestWeeklyWithdrawal(Long deliveryBoyId) {
        validateAndGetDeliveryPartner(deliveryBoyId);

        List<DeliveryAssignment> deliveredAssignments = deliveryAssignmentRepository
                .findByDeliveryBoyIdAndDeliveredAtIsNotNull(deliveryBoyId);

        boolean hasOpenRequest = deliveredAssignments.stream()
                .anyMatch(item -> item.getPayoutStatus() == PayoutStatus.REQUESTED);
        if (hasOpenRequest) {
            throw new BadRequestException("Your weekly withdrawal request is already under review.");
        }

        LocalDateTime latestRequestTime = deliveredAssignments.stream()
                .map(DeliveryAssignment::getPayoutRequestedAt)
                .filter(item -> item != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        if (latestRequestTime != null && LocalDateTime.now().isBefore(latestRequestTime.plusDays(WITHDRAWAL_WINDOW_DAYS))) {
            throw new BadRequestException("Weekly withdrawal can be requested only once every 7 days.");
        }

        List<DeliveryAssignment> availableAssignments = deliveredAssignments.stream()
                .filter(item -> item.getEarningAmount() != null && item.getEarningAmount().compareTo(BigDecimal.ZERO) > 0)
                .filter(item -> item.getPayoutStatus() == null || item.getPayoutStatus() == PayoutStatus.PENDING)
                .toList();

        if (availableAssignments.isEmpty()) {
            throw new BadRequestException("No withdrawable delivery earnings are available right now.");
        }

        LocalDateTime now = LocalDateTime.now();
        for (DeliveryAssignment assignment : availableAssignments) {
            assignment.setPayoutStatus(PayoutStatus.REQUESTED);
            assignment.setPayoutRequestedAt(now);
            deliveryAssignmentRepository.save(assignment);
        }
    }

    @Override
    public void validateDeliveryPartnerForAssignment(Long deliveryBoyId) {
        validateAndGetDeliveryPartner(deliveryBoyId);
    }

    private User validateAndGetDeliveryPartner(Long deliveryBoyId) {
        User deliveryPartner = userRepository.findById(deliveryBoyId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found"));

        if (deliveryPartner.getRole() != UserRole.DELIVERY) {
            throw new UnauthorizedException("Selected account is not a delivery partner.");
        }

        if (deliveryPartner.isBlocked()) {
            throw new UnauthorizedException("This delivery partner is blocked and cannot receive new orders.");
        }

        if (deliveryPartner.getDeliveryStatus() == null) {
            deliveryPartner.setDeliveryStatus(DeliveryBoyStatus.AT_SHOP);
            userRepository.save(deliveryPartner);
        }

        return deliveryPartner;
    }

    private void ensureAvailableForPickup(User deliveryPartner) {
        if (deliveryPartner.getDeliveryStatus() == DeliveryBoyStatus.ON_DELIVERY
                || deliveryPartner.getDeliveryStatus() == DeliveryBoyStatus.RETURNING) {
            throw new BadRequestException("This delivery partner is already on a route and cannot receive new pickup assignments.");
        }
        if (deliveryPartner.getDeliveryStatus() == DeliveryBoyStatus.OFFLINE) {
            throw new BadRequestException("This delivery partner is offline right now.");
        }
    }

    private DeliveryBatch getOrCreatePickupBatch(User deliveryPartner, Order order) {
        DeliveryBatch batch = deliveryBatchRepository
                .findFirstByDeliveryBoyIdAndStatusOrderByIdDesc(deliveryPartner.getId(), DeliveryBatchStatus.ACTIVE)
                .orElse(null);

        if (batch != null && batch.getStartedAt() != null) {
            throw new BadRequestException("This delivery partner has already picked up a live batch and cannot receive new orders right now.");
        }

        if (batch != null) {
            List<Order> batchOrders = orderRepository.findByBatchId(batch.getId());
            if (!batchOrders.isEmpty() && !isSameRoute(batchOrders.get(0), order)) {
                throw new BadRequestException("Only nearby same-route orders can be grouped into the current pickup batch.");
            }
            return batch;
        }

        DeliveryBatch nextBatch = new DeliveryBatch();
        nextBatch.setDeliveryBoyId(deliveryPartner.getId());
        nextBatch.setTotalOrders(0);
        nextBatch.setTotalEarning(BigDecimal.ZERO);
        nextBatch.setStatus(DeliveryBatchStatus.ACTIVE);
        return deliveryBatchRepository.save(nextBatch);
    }

    private boolean isSameRoute(Order anchorOrder, Order nextOrder) {
        if (anchorOrder.getDeliveryLatitude() == null || anchorOrder.getDeliveryLongitude() == null
                || nextOrder.getDeliveryLatitude() == null || nextOrder.getDeliveryLongitude() == null) {
            return true;
        }

        double distanceKm = calculateDistanceKm(
                anchorOrder.getDeliveryLatitude(),
                anchorOrder.getDeliveryLongitude(),
                nextOrder.getDeliveryLatitude(),
                nextOrder.getDeliveryLongitude()
        );
        return distanceKm <= NEARBY_BATCH_RADIUS_KM;
    }

    private boolean deliveryBoyIdEquals(Order order, Long deliveryBoyId) {
        return order.getDeliveryBoyId() != null && order.getDeliveryBoyId().equals(deliveryBoyId);
    }

    private void recalculateBatchTotals(Long batchId) {
        deliveryBatchRepository.findById(batchId).ifPresent(batch -> {
            List<Order> batchOrders = orderRepository.findByBatchId(batchId);
            batch.setTotalOrders(batchOrders.size());
            batch.setTotalEarning(batchOrders.stream()
                    .map(Order::getDeliveryEarning)
                    .filter(item -> item != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            deliveryBatchRepository.save(batch);
        });
    }

    private BigDecimal readDecimalSetting(String key, BigDecimal fallback) {
        try {
            return storeSettingRepository.findBySettingKey(key)
                    .map(StoreSetting::getSettingValue)
                    .filter(value -> value != null && !value.isBlank())
                    .map(BigDecimal::new)
                    .orElse(fallback);
        } catch (Exception exception) {
            return fallback;
        }
    }

    private double calculateDistanceKm(double fromLat, double fromLng, double toLat, double toLng) {
        double earthRadiusKm = 6371D;
        double latDelta = Math.toRadians(toLat - fromLat);
        double lngDelta = Math.toRadians(toLng - fromLng);
        double startLat = Math.toRadians(fromLat);
        double endLat = Math.toRadians(toLat);
        double a = Math.sin(latDelta / 2) * Math.sin(latDelta / 2)
                + Math.cos(startLat) * Math.cos(endLat)
                * Math.sin(lngDelta / 2) * Math.sin(lngDelta / 2);
        return 2 * earthRadiusKm * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
