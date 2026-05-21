package com.akgeneralstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChakkiBookingRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    private String phone;

    @NotBlank
    private String pickupAddress;

    @NotBlank
    private String grainType;

    @Min(1)
    private Integer quantityKg;

    private String preferredSlot;
    private String notes;
}
