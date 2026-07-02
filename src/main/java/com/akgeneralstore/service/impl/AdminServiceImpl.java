package com.akgeneralstore.service.impl;

import com.akgeneralstore.dto.request.AdminDeliveryPartnerRequest;
import com.akgeneralstore.dto.request.CouponRequest;
import com.akgeneralstore.dto.request.DeliveryPayoutRequest;
import com.akgeneralstore.dto.request.StoreSettingsRequest;
import com.akgeneralstore.dto.response.*;
import com.akgeneralstore.entity.Coupon;
import com.akgeneralstore.entity.DeliveryAssignment;
import com.akgeneralstore.entity.DeliveryBatch;
import com.akgeneralstore.entity.Order;
import com.akgeneralstore.entity.Payment;
import com.akgeneralstore.entity.StoreSetting;
import com.akgeneralstore.entity.User;
import com.akgeneralstore.exception.BadRequestException;
import com.akgeneralstore.exception.ResourceNotFoundException;
import com.akgeneralstore.enums.DeliveryBoyStatus;
import com.akgeneralstore.enums.OrderStatus;
import com.akgeneralstore.enums.PayoutStatus;
import com.akgeneralstore.enums.UserRole;
import com.akgeneralstore.repository.CouponRepository;
import com.akgeneralstore.repository.DeliveryAssignmentRepository;
import com.akgeneralstore.repository.DeliveryBatchRepository;
import com.akgeneralstore.repository.OrderRepository;
import com.akgeneralstore.repository.PaymentRepository;
import com.akgeneralstore.repository.ProductRepository;
import com.akgeneralstore.repository.StoreSettingRepository;
import com.akgeneralstore.repository.UserRepository;
import com.akgeneralstore.service.AdminService;
import com.akgeneralstore.service.OrderService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final DeliveryBatchRepository deliveryBatchRepository;
    private final PaymentRepository paymentRepository;
    private final CouponRepository couponRepository;
    private final StoreSettingRepository storeSettingRepository;
    private final OrderService orderService;
    private final PasswordEncoder passwordEncoder;

    public AdminServiceImpl(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            DeliveryAssignmentRepository deliveryAssignmentRepository,
            DeliveryBatchRepository deliveryBatchRepository,
            PaymentRepository paymentRepository,
            CouponRepository couponRepository,
            StoreSettingRepository storeSettingRepository,
            OrderService orderService,
            PasswordEncoder passwordEncoder
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.deliveryBatchRepository = deliveryBatchRepository;
        this.paymentRepository = paymentRepository;
        this.couponRepository = couponRepository;
        this.storeSettingRepository = storeSettingRepository;
        this.orderService = orderService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public DashboardResponse getDashboard() {
        BigDecimal totalRevenue = orderRepository.findAll().stream()
                .map(order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<OrderResponse> recentOrders = orderRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(order -> orderService.getAllOrders().stream()
                        .filter(response -> response.getOrderId().equals(order.getId()))
                        .findFirst()
                        .orElse(null))
                .filter(item -> item != null)
                .toList();

        return DashboardResponse.builder()
                .totalOrders(orderRepository.count())
                .totalRevenue(totalRevenue)
                .productsCount(productRepository.count())
                .customersCount(userRepository.countByRole(UserRole.CUSTOMER))
                .recentOrders(recentOrders)
                .build();
    }

    @Override
    public List<UserSummaryResponse> getCustomers() {
        return userRepository.findByRole(UserRole.CUSTOMER).stream()
                .map(this::mapCustomerUser)
                .toList();
    }

    @Override
    public UserSummaryResponse updateCustomerBlockedStatus(Long userId, boolean blocked) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (user.getRole() != UserRole.CUSTOMER) {
            throw new BadRequestException("Only customer accounts can be blocked or unblocked.");
        }

        user.setBlocked(blocked);
        return mapCustomerUser(userRepository.save(user));
    }

    @Override
    public void deleteCustomer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (user.getRole() != UserRole.CUSTOMER) {
            throw new BadRequestException("Only customer accounts can be deleted.");
        }

        userRepository.delete(user);
    }

    @Override
    public List<UserSummaryResponse> getDeliveryTeam() {
        return userRepository.findByRole(UserRole.DELIVERY).stream()
                .map(this::mapDeliveryUser)
                .toList();
    }

    @Override
    public UserSummaryResponse getDeliveryTeamMember(Long userId) {
        return mapDeliveryUser(getDeliveryUserOrThrow(userId));
    }

    @Override
    public UserSummaryResponse createDeliveryPartner(AdminDeliveryPartnerRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getPhone().trim();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("Email already registered");
        }

        if (userRepository.findByPhone(phone).isPresent()) {
            throw new BadRequestException("Phone already registered");
        }

        if (request.getPassword() == null || request.getPassword().trim().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters.");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(UserRole.DELIVERY);
        user.setEmailVerified(true);
        user.setBlocked(false);
        user.setDeliveryStatus(DeliveryBoyStatus.AT_SHOP);
        user.setPassword(passwordEncoder.encode(request.getPassword().trim()));

        return mapDeliveryUser(userRepository.save(user));
    }

    @Override
    public UserSummaryResponse updateDeliveryPartnerBlockedStatus(Long userId, boolean blocked) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found"));

        if (user.getRole() != UserRole.DELIVERY) {
            throw new BadRequestException("Only delivery partners can be blocked or unblocked.");
        }

        user.setBlocked(blocked);
        return mapDeliveryUser(userRepository.save(user));
    }

    @Override
    public UserSummaryResponse markDeliveryPayoutPaid(Long userId, DeliveryPayoutRequest request) {
        User user = getDeliveryUserOrThrow(userId);

        List<DeliveryAssignment> assignments = deliveryAssignmentRepository.findByDeliveryBoyId(userId).stream()
                .filter(item -> item.getDeliveredAt() != null)
                .filter(item -> item.getPayoutStatus() == PayoutStatus.REQUESTED)
                .toList();

        if (assignments.isEmpty()) {
            throw new BadRequestException("No weekly payout request is waiting for settlement.");
        }

        LocalDateTime now = LocalDateTime.now();
        for (DeliveryAssignment assignment : assignments) {
            assignment.setPayoutStatus(PayoutStatus.PAID);
            assignment.setPayoutRequestedAt(
                    assignment.getPayoutRequestedAt() == null ? now : assignment.getPayoutRequestedAt()
            );
            assignment.setPayoutReference(
                    request != null && request.getReferenceId() != null && !request.getReferenceId().isBlank()
                            ? request.getReferenceId().trim()
                            : "PAYOUT-" + userId + "-" + System.currentTimeMillis()
            );
            assignment.setPayoutPaidAt(now);
            deliveryAssignmentRepository.save(assignment);
        }

        return mapDeliveryUser(user);
    }

    @Override
    public List<CouponResponse> getCoupons() {
        return couponRepository.findAll().stream().map(this::mapCoupon).toList();
    }

    @Override
    public CouponResponse createCoupon(CouponRequest request) {
        Coupon coupon = toCoupon(new Coupon(), request);
        return mapCoupon(couponRepository.save(coupon));
    }

    @Override
    public CouponResponse updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new com.akgeneralstore.exception.ResourceNotFoundException("Coupon not found"));
        return mapCoupon(couponRepository.save(toCoupon(coupon, request)));
    }

    @Override
    public void deleteCoupon(Long id) {
        if (!couponRepository.existsById(id)) {
            throw new com.akgeneralstore.exception.ResourceNotFoundException("Coupon not found");
        }
        couponRepository.deleteById(id);
    }

    @Override
    public ReportResponse getReports() {
        LocalDate today = LocalDate.now();
        List<Order> allOrders = orderRepository.findAll();
        List<Order> todayOrders = allOrders.stream()
                .filter(order -> order.getCreatedAt() != null && order.getCreatedAt().toLocalDate().equals(today))
                .toList();

        BigDecimal todayRevenue = todayOrders.stream()
                .map(order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingOrders = allOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.CONFIRMED)
                .count();

        return ReportResponse.builder()
                .todayRevenue(todayRevenue)
                .todayOrders(todayOrders.size())
                .pendingOrders(pendingOrders)
                .reportHighlights(List.of(
                        "Daily sales overview generated from live orders",
                        "Pending order count for fulfilment review",
                        "Use recent order trends to plan inventory and delivery capacity"
                ))
                .build();
    }

    @Override
    public StoreSettingsResponse getSettings() {
        return StoreSettingsResponse.builder()
                .storeName(getSetting("store_name", "AK General Store"))
                .supportPhone(getSetting("support_phone", "9483989109"))
                .supportEmail(getSetting("support_email", "support@akgeneralstore.com"))
                .freeDeliveryThreshold(getSetting("free_delivery_threshold", "499"))
                .deliveryCharge(getSetting("delivery_charge", "40"))
                .enabledPayments(getSetting("enabled_payments", "COD,UPI,RAZORPAY"))
                .serviceRadiusKm(getSetting("service_radius_km", "25"))
                .storeLocations(getSetting("store_locations", "AK General Store Main|28.0162|74.9642|25|https://maps.app.goo.gl/YY4f8NfB9sTfRQrH7"))
                .upiMerchantName(getSetting("upi_merchant_name", "AK General Store"))
                .upiId(getSetting("upi_id", "support@akgeneralstore"))
                .deliveryBasePayoutAmount(getSetting("delivery_base_payout_amount", "20"))
                .deliveryAdditionalPayoutAmount(getSetting("delivery_additional_payout_amount", "10"))
                .build();
    }

    @Override
    public StoreSettingsResponse updateSettings(StoreSettingsRequest request) {
        saveSetting("store_name", request.getStoreName());
        saveSetting("support_phone", request.getSupportPhone());
        saveSetting("support_email", request.getSupportEmail());
        saveSetting("free_delivery_threshold", request.getFreeDeliveryThreshold());
        saveSetting("delivery_charge", request.getDeliveryCharge());
        saveSetting("enabled_payments", request.getEnabledPayments());
        saveSetting("service_radius_km", request.getServiceRadiusKm());
        saveSetting("store_locations", request.getStoreLocations());
        saveSetting("upi_merchant_name", request.getUpiMerchantName());
        saveSetting("upi_id", request.getUpiId());
        saveSetting("delivery_base_payout_amount", request.getDeliveryBasePayoutAmount());
        saveSetting("delivery_additional_payout_amount", request.getDeliveryAdditionalPayoutAmount());
        return getSettings();
    }

    private Coupon toCoupon(Coupon coupon, CouponRequest request) {
        coupon.setCode(request.getCode() == null ? null : request.getCode().trim().toUpperCase());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinimumOrderAmount(request.getMinimumOrderAmount());
        coupon.setMaxUsesPerUser(request.getMaxUsesPerUser());
        coupon.setMaxTotalUses(request.getMaxTotalUses());
        coupon.setExpiryDate(request.getExpiryDate());
        coupon.setFirstOrderOnly(request.isFirstOrderOnly());
        coupon.setActive(request.isActive());
        return coupon;
    }

    private CouponResponse mapCoupon(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minimumOrderAmount(coupon.getMinimumOrderAmount())
                .maxUsesPerUser(coupon.getMaxUsesPerUser())
                .maxTotalUses(coupon.getMaxTotalUses())
                .expiryDate(coupon.getExpiryDate())
                .firstOrderOnly(coupon.isFirstOrderOnly())
                .active(coupon.isActive())
                .currentTotalUses(orderRepository.countByCouponCodeIgnoreCase(coupon.getCode()))
                .build();
    }

    private String getSetting(String key, String fallback) {
        return storeSettingRepository.findBySettingKey(key)
                .map(StoreSetting::getSettingValue)
                .orElse(fallback);
    }

    private void saveSetting(String key, String value) {
        StoreSetting setting = storeSettingRepository.findBySettingKey(key).orElseGet(StoreSetting::new);
        setting.setSettingKey(key);
        setting.setSettingValue(value == null ? "" : value);
        storeSettingRepository.save(setting);
    }

    private UserSummaryResponse mapDeliveryUser(User user) {
        List<DeliveryAssignment> assignments = deliveryAssignmentRepository.findByDeliveryBoyId(user.getId());
        DeliveryBatch activeBatch = deliveryBatchRepository
                .findFirstByDeliveryBoyIdAndStatusOrderByIdDesc(user.getId(), com.akgeneralstore.enums.DeliveryBatchStatus.ACTIVE)
                .orElse(null);
        List<DeliveryAssignment> deliveredAssignments = assignments.stream()
                .filter(item -> item.getDeliveredAt() != null)
                .toList();
        long deliveryAssignments = assignments.size();
        DeliveryBoyStatus deliveryBoyStatus = user.getDeliveryStatus() == null
                ? DeliveryBoyStatus.AT_SHOP
                : user.getDeliveryStatus();
        String status = user.isBlocked() ? "Blocked" : deliveryBoyStatus.name().replace("_", " ");

        BigDecimal totalCollected = deliveredAssignments.stream()
                .map(DeliveryAssignment::getCollectedAmount)
                .filter(item -> item != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cashCollected = deliveredAssignments.stream()
                .map(DeliveryAssignment::getCashCollectedAmount)
                .filter(item -> item != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal upiCollected = deliveredAssignments.stream()
                .map(DeliveryAssignment::getUpiCollectedAmount)
                .filter(item -> item != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEarnings = deliveredAssignments.stream()
                .map(DeliveryAssignment::getEarningAmount)
                .filter(item -> item != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal availableForWithdrawal = deliveredAssignments.stream()
                .filter(item -> item.getPayoutStatus() == null || item.getPayoutStatus() == PayoutStatus.PENDING)
                .map(DeliveryAssignment::getEarningAmount)
                .filter(item -> item != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal requestedPayout = deliveredAssignments.stream()
                .filter(item -> item.getPayoutStatus() == PayoutStatus.REQUESTED)
                .map(DeliveryAssignment::getEarningAmount)
                .filter(item -> item != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingPayout = availableForWithdrawal.add(requestedPayout);
        BigDecimal paidOut = deliveredAssignments.stream()
                .filter(item -> item.getPayoutStatus() == PayoutStatus.PAID)
                .map(DeliveryAssignment::getEarningAmount)
                .filter(item -> item != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingCollection = assignments.stream()
                .map(DeliveryAssignment::getOrderId)
                .map(orderRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(order -> order.getStatus() == OrderStatus.OUT_FOR_DELIVERY || order.getStatus() == OrderStatus.PICKED_UP)
                .filter(order -> order.getPaymentMode() == com.akgeneralstore.enums.PaymentMode.COD)
                .map(order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime lastPayoutRequestedAt = deliveredAssignments.stream()
                .map(DeliveryAssignment::getPayoutRequestedAt)
                .filter(item -> item != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime nextWithdrawalAvailableAt = lastPayoutRequestedAt == null
                ? null
                : lastPayoutRequestedAt.plusDays(7);
        boolean withdrawalEligible = requestedPayout.compareTo(BigDecimal.ZERO) <= 0
                && availableForWithdrawal.compareTo(BigDecimal.ZERO) > 0
                && (nextWithdrawalAvailableAt == null || !LocalDateTime.now().isBefore(nextWithdrawalAvailableAt));

        return UserSummaryResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .ordersCount(deliveryAssignments)
                .status(status)
                .blocked(user.isBlocked())
                .activeBatchId(activeBatch != null ? activeBatch.getId() : null)
                .activeBatchOrders(activeBatch != null ? activeBatch.getTotalOrders() : 0)
                .activeBatchTotalEarning(activeBatch != null ? activeBatch.getTotalEarning() : BigDecimal.ZERO)
                .deliveryBoyStatus(deliveryBoyStatus)
                .totalCollectedAmount(totalCollected)
                .cashCollectedAmount(cashCollected)
                .upiCollectedAmount(upiCollected)
                .pendingCollectionAmount(pendingCollection)
                .totalEarningAmount(totalEarnings)
                .pendingPayoutAmount(pendingPayout)
                .availableForWithdrawalAmount(availableForWithdrawal)
                .requestedPayoutAmount(requestedPayout)
                .paidOutAmount(paidOut)
                .withdrawalEligible(withdrawalEligible)
                .lastPayoutRequestedAt(lastPayoutRequestedAt)
                .nextWithdrawalAvailableAt(nextWithdrawalAvailableAt)
                .build();
    }

    private UserSummaryResponse mapCustomerUser(User user) {
        long ordersCount = orderRepository.findByUserId(user.getId()).size();
        return UserSummaryResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .ordersCount(ordersCount)
                .status(user.isBlocked() ? "Blocked" : ordersCount > 0 ? "Ordering" : "Active")
                .blocked(user.isBlocked())
                .totalCollectedAmount(BigDecimal.ZERO)
                .cashCollectedAmount(BigDecimal.ZERO)
                .upiCollectedAmount(BigDecimal.ZERO)
                .pendingCollectionAmount(BigDecimal.ZERO)
                .totalEarningAmount(BigDecimal.ZERO)
                .pendingPayoutAmount(BigDecimal.ZERO)
                .availableForWithdrawalAmount(BigDecimal.ZERO)
                .requestedPayoutAmount(BigDecimal.ZERO)
                .paidOutAmount(BigDecimal.ZERO)
                .withdrawalEligible(false)
                .lastPayoutRequestedAt(null)
                .nextWithdrawalAvailableAt(null)
                .build();
    }

    private User getDeliveryUserOrThrow(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery partner not found"));

        if (user.getRole() != UserRole.DELIVERY) {
            throw new BadRequestException("Only delivery partners can use this workflow.");
        }

        return user;
    }
}
