package com.akgeneralstore.controller;

import com.akgeneralstore.dto.response.ApiResponse;
import com.akgeneralstore.dto.request.AdminDeliveryPartnerRequest;
import com.akgeneralstore.dto.request.CouponRequest;
import com.akgeneralstore.dto.request.DeliveryPayoutRequest;
import com.akgeneralstore.dto.request.StoreSettingsRequest;
import com.akgeneralstore.dto.response.CouponResponse;
import com.akgeneralstore.dto.response.DashboardResponse;
import com.akgeneralstore.dto.response.OrderResponse;
import com.akgeneralstore.dto.response.ReportResponse;
import com.akgeneralstore.dto.response.StoreSettingsResponse;
import com.akgeneralstore.dto.response.UserSummaryResponse;
import com.akgeneralstore.enums.OrderStatus;
import com.akgeneralstore.service.AdminService;
import com.akgeneralstore.service.DeliveryService;
import com.akgeneralstore.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final OrderService orderService;
    private final DeliveryService deliveryService;

    public AdminController(AdminService adminService, OrderService orderService, DeliveryService deliveryService) {
        this.adminService = adminService;
        this.orderService = orderService;
        this.deliveryService = deliveryService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardResponse> getDashboard() {
        return new ApiResponse<>(true, "Dashboard fetched", adminService.getDashboard());
    }

    @GetMapping("/customers")
    public ApiResponse<List<UserSummaryResponse>> getCustomers() {
        return new ApiResponse<>(true, "Customers fetched", adminService.getCustomers());
    }

    @PutMapping("/customers/{userId}/blocked")
    public ApiResponse<UserSummaryResponse> updateCustomerBlockedStatus(
            @PathVariable Long userId,
            @RequestParam boolean blocked
    ) {
        return new ApiResponse<>(true, blocked ? "Customer blocked" : "Customer unblocked", adminService.updateCustomerBlockedStatus(userId, blocked));
    }

    @DeleteMapping("/customers/{userId}")
    public ApiResponse<Void> deleteCustomer(@PathVariable Long userId) {
        adminService.deleteCustomer(userId);
        return new ApiResponse<>(true, "Customer deleted", null);
    }

    @GetMapping("/delivery-team")
    public ApiResponse<List<UserSummaryResponse>> getDeliveryTeam() {
        return new ApiResponse<>(true, "Delivery team fetched", adminService.getDeliveryTeam());
    }

    @GetMapping("/delivery-team/{userId}")
    public ApiResponse<UserSummaryResponse> getDeliveryTeamMember(@PathVariable Long userId) {
        return new ApiResponse<>(true, "Delivery partner fetched", adminService.getDeliveryTeamMember(userId));
    }

    @PostMapping("/delivery-team")
    public ApiResponse<UserSummaryResponse> createDeliveryTeamMember(@Valid @RequestBody AdminDeliveryPartnerRequest request) {
        return new ApiResponse<>(true, "Delivery partner created", adminService.createDeliveryPartner(request));
    }

    @PutMapping("/delivery-team/{userId}/blocked")
    public ApiResponse<UserSummaryResponse> updateDeliveryTeamBlockedStatus(
            @PathVariable Long userId,
            @RequestParam boolean blocked
    ) {
        return new ApiResponse<>(true, blocked ? "Delivery partner blocked" : "Delivery partner unblocked", adminService.updateDeliveryPartnerBlockedStatus(userId, blocked));
    }

    @PostMapping("/delivery-team/{userId}/payout")
    public ApiResponse<UserSummaryResponse> markDeliveryPayoutPaid(
            @PathVariable Long userId,
            @RequestBody(required = false) DeliveryPayoutRequest request
    ) {
        return new ApiResponse<>(true, "Delivery payout marked as paid", adminService.markDeliveryPayoutPaid(userId, request));
    }

    @GetMapping("/coupons")
    public ApiResponse<List<CouponResponse>> getCoupons() {
        return new ApiResponse<>(true, "Coupons fetched", adminService.getCoupons());
    }

    @PostMapping("/coupons")
    public ApiResponse<CouponResponse> createCoupon(@RequestBody CouponRequest request) {
        return new ApiResponse<>(true, "Coupon created", adminService.createCoupon(request));
    }

    @PutMapping("/coupons/{id}")
    public ApiResponse<CouponResponse> updateCoupon(@PathVariable Long id, @RequestBody CouponRequest request) {
        return new ApiResponse<>(true, "Coupon updated", adminService.updateCoupon(id, request));
    }

    @DeleteMapping("/coupons/{id}")
    public ApiResponse<Void> deleteCoupon(@PathVariable Long id) {
        adminService.deleteCoupon(id);
        return new ApiResponse<>(true, "Coupon deleted", null);
    }

    @GetMapping("/reports")
    public ApiResponse<ReportResponse> getReports() {
        return new ApiResponse<>(true, "Reports fetched", adminService.getReports());
    }

    @GetMapping("/settings")
    public ApiResponse<StoreSettingsResponse> getSettings() {
        return new ApiResponse<>(true, "Settings fetched", adminService.getSettings());
    }

    @PutMapping("/settings")
    public ApiResponse<StoreSettingsResponse> updateSettings(@RequestBody StoreSettingsRequest request) {
        return new ApiResponse<>(true, "Settings updated", adminService.updateSettings(request));
    }

    @GetMapping("/orders")
    public ApiResponse<List<OrderResponse>> getOrders() {
        return new ApiResponse<>(true, "Orders fetched", orderService.getAllOrders());
    }

    @PutMapping("/orders/{id}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        return new ApiResponse<>(true, "Order status updated", orderService.updateOrderStatus(id, status));
    }

    @PutMapping("/orders/{id}/assign")
    public ApiResponse<OrderResponse> assignOrder(@PathVariable Long id, @RequestParam(defaultValue = "3") Long deliveryBoyId) {
        return new ApiResponse<>(true, "Order assigned to delivery", deliveryService.assignOrder(id, deliveryBoyId));
    }
}
