package com.akgeneralstore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardResponse {

    private long totalOrders;
    private BigDecimal totalRevenue;
    private long productsCount;
    private long customersCount;
    private List<OrderResponse> recentOrders;
}
