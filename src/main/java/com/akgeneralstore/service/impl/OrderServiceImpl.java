package com.akgeneralstore.service.impl;

import com.akgeneralstore.dto.request.OrderRequest;
import com.akgeneralstore.dto.response.OrderResponse;
import com.akgeneralstore.entity.Address;
import com.akgeneralstore.entity.DeliveryBatch;
import com.akgeneralstore.entity.Order;
import com.akgeneralstore.entity.OrderItem;
import com.akgeneralstore.entity.Payment;
import com.akgeneralstore.entity.Product;
import com.akgeneralstore.entity.StoreSetting;
import com.akgeneralstore.entity.User;
import com.akgeneralstore.enums.OrderStatus;
import com.akgeneralstore.enums.PaymentStatus;
import com.akgeneralstore.exception.BadRequestException;
import com.akgeneralstore.exception.ResourceNotFoundException;
import com.akgeneralstore.repository.AddressRepository;
import com.akgeneralstore.repository.DeliveryAssignmentRepository;
import com.akgeneralstore.repository.DeliveryBatchRepository;
import com.akgeneralstore.repository.OrderItemRepository;
import com.akgeneralstore.repository.OrderRepository;
import com.akgeneralstore.repository.PaymentRepository;
import com.akgeneralstore.repository.ProductRepository;
import com.akgeneralstore.repository.StoreSettingRepository;
import com.akgeneralstore.repository.UserRepository;
import com.akgeneralstore.service.CouponService;
import com.akgeneralstore.service.OrderService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final DeliveryBatchRepository deliveryBatchRepository;
    private final CouponService couponService;
    private final AddressRepository addressRepository;
    private final StoreSettingRepository storeSettingRepository;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            DeliveryAssignmentRepository deliveryAssignmentRepository,
            DeliveryBatchRepository deliveryBatchRepository,
            CouponService couponService,
            AddressRepository addressRepository,
            StoreSettingRepository storeSettingRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.deliveryBatchRepository = deliveryBatchRepository;
        this.couponService = couponService;
        this.addressRepository = addressRepository;
        this.storeSettingRepository = storeSettingRepository;
    }

    @Override
    public OrderResponse placeOrder(OrderRequest request) {
        DeliveryCoverageResult coverage = validateCoverage(request);

        Order order = new Order();
        order.setOrderNumber("AK" + System.currentTimeMillis());
        order.setUserId(request.getUserId() == null ? 1L : request.getUserId());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMode(request.getPaymentMode());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setDeliveryLatitude(coverage.latitude());
        order.setDeliveryLongitude(coverage.longitude());
        order.setDeliveryLocationLabel(coverage.locationLabel());
        order.setServingStoreName(coverage.storeName());
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemRequest.getProductId()));
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            subtotal = subtotal.add(lineTotal);
        }
        BigDecimal deliveryFee = request.getDeliveryFee() == null ? BigDecimal.ZERO : request.getDeliveryFee().max(BigDecimal.ZERO);
        BigDecimal discountAmount = resolveDiscountAmount(request, subtotal);
        BigDecimal finalTotal = subtotal.add(deliveryFee).subtract(discountAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        order.setSubtotalAmount(subtotal.setScale(2, RoundingMode.HALF_UP));
        order.setDeliveryFee(deliveryFee.setScale(2, RoundingMode.HALF_UP));
        order.setDiscountAmount(discountAmount.setScale(2, RoundingMode.HALF_UP));
        order.setCouponCode(request.getCouponCode() == null || request.getCouponCode().isBlank() ? null : request.getCouponCode().trim().toUpperCase());
        order.setTotalAmount(finalTotal);
        Order savedOrder = orderRepository.save(order);

        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemRequest.getProductId()));
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItemRepository.save(orderItem);
        }

        if (request.getPaymentMode() != null) {
            Payment payment = new Payment();
            payment.setOrderId(savedOrder.getId());
            payment.setTransactionId("PAY-" + savedOrder.getOrderNumber());
            payment.setAmount(finalTotal);
            payment.setMode(request.getPaymentMode());
            payment.setStatus(PaymentStatus.PENDING);
            payment.setCreatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }

        return mapOrder(savedOrder);
    }

    @Override
    public List<OrderResponse> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId).stream().map(this::mapOrder).toList();
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream().map(this::mapOrder).toList();
    }

    @Override
    public OrderResponse cancelOrder(Long userId, String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException("You can only cancel your own orders.");
        }

        if (!isCancellableByCustomer(order.getStatus())) {
            throw new BadRequestException("This order can no longer be cancelled.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        return mapOrder(orderRepository.save(order));
    }

    @Override
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setStatus(status);
        if (status == OrderStatus.DELIVERED && order.getPaymentMode() == com.akgeneralstore.enums.PaymentMode.COD) {
            order.setPaymentStatus(PaymentStatus.SUCCESS);
        }
        return mapOrder(orderRepository.save(order));
    }

    private OrderResponse mapOrder(Order order) {
        User customer = userRepository.findById(order.getUserId()).orElse(null);
        Optional<com.akgeneralstore.entity.DeliveryAssignment> assignmentOptional = deliveryAssignmentRepository.findByOrderId(order.getId());
        User deliveryPartner = assignmentOptional
                .flatMap(assignment -> userRepository.findById(assignment.getDeliveryBoyId()))
                .orElse(null);
        if (deliveryPartner == null && order.getDeliveryBoyId() != null) {
            deliveryPartner = userRepository.findById(order.getDeliveryBoyId()).orElse(null);
        }
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        com.akgeneralstore.entity.DeliveryAssignment assignment = assignmentOptional.orElse(null);
        DeliveryBatch batch = order.getBatchId() == null ? null : deliveryBatchRepository.findById(order.getBatchId()).orElse(null);

        List<String> itemNames = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(OrderItem::getProductName)
                .toList();
        return OrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUserId())
                .customerName(customer != null ? customer.getName() : "AK Customer")
                .customerEmail(customer != null ? customer.getEmail() : "")
                .customerPhone(customer != null ? customer.getPhone() : "")
                .status(order.getStatus())
                .deliveryBoyId(order.getDeliveryBoyId())
                .batchId(order.getBatchId())
                .totalAmount(order.getTotalAmount())
                .subtotalAmount(order.getSubtotalAmount())
                .deliveryFee(order.getDeliveryFee())
                .discountAmount(order.getDiscountAmount())
                .couponCode(order.getCouponCode())
                .paymentMode(order.getPaymentMode())
                .paymentStatus(order.getPaymentStatus())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryLatitude(order.getDeliveryLatitude())
                .deliveryLongitude(order.getDeliveryLongitude())
                .deliveryLocationLabel(order.getDeliveryLocationLabel())
                .servingStoreName(order.getServingStoreName())
                .batchTotalOrders(batch != null ? batch.getTotalOrders() : null)
                .batchTotalEarning(batch != null ? batch.getTotalEarning() : null)
                .batchStatus(batch != null ? batch.getStatus() : null)
                .deliveryBoyStatus(deliveryPartner != null ? deliveryPartner.getDeliveryStatus() : null)
                .createdAt(order.getCreatedAt())
                .assignedDeliveryName(deliveryPartner != null ? deliveryPartner.getName() : "")
                .assignedDeliveryEmail(deliveryPartner != null ? deliveryPartner.getEmail() : "")
                .assignedDeliveryPhone(deliveryPartner != null ? deliveryPartner.getPhone() : "")
                .collectionMethod(payment != null ? payment.getCollectionMethod() : null)
                .collectedAmount(assignment != null ? assignment.getCollectedAmount() : null)
                .cashCollectedAmount(assignment != null ? assignment.getCashCollectedAmount() : null)
                .upiCollectedAmount(assignment != null ? assignment.getUpiCollectedAmount() : null)
                .collectedByDeliveryBoyId(payment != null ? payment.getCollectedByDeliveryBoyId() : null)
                .collectedAt(payment != null ? payment.getCollectedAt() : null)
                .deliveryEarningAmount(assignment != null && assignment.getEarningAmount() != null ? assignment.getEarningAmount() : order.getDeliveryEarning())
                .payoutStatus(assignment != null ? assignment.getPayoutStatus() : null)
                .payoutRequestedAt(assignment != null ? assignment.getPayoutRequestedAt() : null)
                .payoutReference(assignment != null ? assignment.getPayoutReference() : null)
                .payoutPaidAt(assignment != null ? assignment.getPayoutPaidAt() : null)
                .itemNames(itemNames)
                .build();
    }

    private boolean isCancellableByCustomer(OrderStatus status) {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }

    private BigDecimal resolveDiscountAmount(OrderRequest request, BigDecimal subtotal) {
        if (request.getCouponCode() == null || request.getCouponCode().isBlank()) {
            return BigDecimal.ZERO;
        }

        BigDecimal safeDiscount = couponService
                .validateCoupon(request.getCouponCode().trim(), subtotal, request.getUserId())
                .getDiscountAmount()
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal requestDiscount = request.getDiscountAmount() == null
                ? safeDiscount
                : request.getDiscountAmount().setScale(2, RoundingMode.HALF_UP);

        if (requestDiscount.compareTo(safeDiscount) != 0) {
            throw new BadRequestException("Coupon discount validation failed. Please apply the coupon again.");
        }

        return safeDiscount;
    }

    private DeliveryCoverageResult validateCoverage(OrderRequest request) {
        Address selectedAddress = null;
        if (request.getAddressId() != null) {
            selectedAddress = addressRepository.findByIdAndUserId(request.getAddressId(), request.getUserId())
                    .orElseThrow(() -> new BadRequestException("Selected delivery address could not be found."));
        }

        Double latitude = request.getDeliveryLatitude();
        Double longitude = request.getDeliveryLongitude();
        String locationLabel = request.getDeliveryLocationLabel();

        if ((latitude == null || longitude == null) && selectedAddress != null) {
            latitude = selectedAddress.getLatitude();
            longitude = selectedAddress.getLongitude();
            if (locationLabel == null || locationLabel.isBlank()) {
                locationLabel = selectedAddress.getLocationLabel();
            }
        }

        if (latitude == null || longitude == null) {
            throw new BadRequestException(
                    "Please select an address with a saved delivery location pin before placing the order."
            );
        }

        if (locationLabel == null || locationLabel.isBlank()) {
            locationLabel = request.getDeliveryAddress();
        }

        double defaultRadiusKm = readDoubleSetting("service_radius_km", 25D);
        List<StoreLocation> stores = parseStoreLocations(
                getSetting("store_locations", "AK General Store Main|25.5941|85.1608|25"),
                defaultRadiusKm
        );

        if (stores.isEmpty()) {
            throw new BadRequestException("Store coverage is being updated. Please try again shortly.");
        }

        StoreLocation matchedStore = null;
        double nearestDistance = Double.MAX_VALUE;
        for (StoreLocation store : stores) {
            double distanceKm = calculateDistanceKm(latitude, longitude, store.latitude(), store.longitude());
            if (distanceKm < nearestDistance) {
                nearestDistance = distanceKm;
            }
            if (distanceKm <= store.radiusKm()) {
                matchedStore = new StoreLocation(
                        store.name(),
                        store.latitude(),
                        store.longitude(),
                        store.radiusKm(),
                        store.mapUrl()
                );
                break;
            }
        }

        if (matchedStore == null) {
            long roundedRadius = Math.round(defaultRadiusKm);
            throw new BadRequestException(
                    "Currently, no store is available within " + roundedRadius
                            + " KM of your selected delivery address. We'll be available in your area soon \uD83D\uDE80"
            );
        }

        return new DeliveryCoverageResult(
                latitude,
                longitude,
                locationLabel,
                matchedStore.name()
        );
    }

    private List<StoreLocation> parseStoreLocations(String rawValue, double defaultRadiusKm) {
        List<StoreLocation> stores = new ArrayList<>();
        if (rawValue == null || rawValue.isBlank()) {
            return stores;
        }

        String[] lines = rawValue.split("\\r?\\n");
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            String[] parts = line.split("\\|");
            if (parts.length < 4) {
                continue;
            }

            try {
                String name = parts[0].trim();
                double latitude = Double.parseDouble(parts[1].trim());
                double longitude = Double.parseDouble(parts[2].trim());
                double radiusKm = Double.parseDouble(parts[3].trim().isBlank() ? String.valueOf(defaultRadiusKm) : parts[3].trim());
                String mapUrl = parts.length > 4 ? parts[4].trim() : "";
                stores.add(new StoreLocation(name, latitude, longitude, radiusKm, mapUrl));
            } catch (NumberFormatException ignored) {
                // Skip invalid shop entries instead of breaking checkout for all stores.
            }
        }

        return stores;
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

    private String getSetting(String key, String fallback) {
        return storeSettingRepository.findBySettingKey(key)
                .map(StoreSetting::getSettingValue)
                .orElse(fallback);
    }

    private double readDoubleSetting(String key, double fallback) {
        try {
            return Double.parseDouble(getSetting(key, String.valueOf(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private record StoreLocation(
            String name,
            double latitude,
            double longitude,
            double radiusKm,
            String mapUrl
    ) {
    }

    private record DeliveryCoverageResult(
            double latitude,
            double longitude,
            String locationLabel,
            String storeName
    ) {
    }
}
