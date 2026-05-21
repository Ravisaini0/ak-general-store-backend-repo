package com.akgeneralstore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ReportResponse {
    private BigDecimal todayRevenue;
    private long todayOrders;
    private long pendingOrders;
    private List<String> reportHighlights;
}
