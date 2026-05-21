package com.akgeneralstore.dto.response;

import com.akgeneralstore.enums.UserRole;
import com.akgeneralstore.enums.DeliveryBoyStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class UserSummaryResponse {

    private Long userId;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
    private long ordersCount;
    private String status;
    private boolean blocked;
    private Long activeBatchId;
    private Integer activeBatchOrders;
    private BigDecimal activeBatchTotalEarning;
    private DeliveryBoyStatus deliveryBoyStatus;
    private BigDecimal totalCollectedAmount;
    private BigDecimal cashCollectedAmount;
    private BigDecimal upiCollectedAmount;
    private BigDecimal pendingCollectionAmount;
    private BigDecimal totalEarningAmount;
    private BigDecimal pendingPayoutAmount;
    private BigDecimal availableForWithdrawalAmount;
    private BigDecimal requestedPayoutAmount;
    private BigDecimal paidOutAmount;
    private boolean withdrawalEligible;
    private LocalDateTime lastPayoutRequestedAt;
    private LocalDateTime nextWithdrawalAvailableAt;
}
