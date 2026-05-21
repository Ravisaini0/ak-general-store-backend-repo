package com.akgeneralstore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChakkiBookingResponse {
    private Long id;
    private Long userId;
    private String customerName;
    private String customerEmail;
    private String fullName;
    private String phone;
    private String pickupAddress;
    private String grainType;
    private Integer quantityKg;
    private String preferredSlot;
    private String notes;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
