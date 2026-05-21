package com.akgeneralstore.service;

import com.akgeneralstore.dto.request.DeliveryPayoutRequest;
import com.akgeneralstore.dto.request.CouponRequest;
import com.akgeneralstore.dto.request.AdminDeliveryPartnerRequest;
import com.akgeneralstore.dto.request.StoreSettingsRequest;
import com.akgeneralstore.dto.response.CouponResponse;
import com.akgeneralstore.dto.response.DashboardResponse;
import com.akgeneralstore.dto.response.ReportResponse;
import com.akgeneralstore.dto.response.StoreSettingsResponse;
import com.akgeneralstore.dto.response.UserSummaryResponse;

import java.util.List;

public interface AdminService {
    DashboardResponse getDashboard();
    List<UserSummaryResponse> getCustomers();
    UserSummaryResponse updateCustomerBlockedStatus(Long userId, boolean blocked);
    void deleteCustomer(Long userId);
    List<UserSummaryResponse> getDeliveryTeam();
    UserSummaryResponse getDeliveryTeamMember(Long userId);
    UserSummaryResponse createDeliveryPartner(AdminDeliveryPartnerRequest request);
    UserSummaryResponse updateDeliveryPartnerBlockedStatus(Long userId, boolean blocked);
    UserSummaryResponse markDeliveryPayoutPaid(Long userId, DeliveryPayoutRequest request);
    List<CouponResponse> getCoupons();
    CouponResponse createCoupon(CouponRequest request);
    CouponResponse updateCoupon(Long id, CouponRequest request);
    void deleteCoupon(Long id);
    ReportResponse getReports();
    StoreSettingsResponse getSettings();
    StoreSettingsResponse updateSettings(StoreSettingsRequest request);
}
