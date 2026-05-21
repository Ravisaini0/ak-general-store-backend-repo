package com.akgeneralstore.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressResponse {
    private Long id;
    private String fullName;
    private String phone;
    private String addressType;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String pincode;
    private String landmark;
    private Double latitude;
    private Double longitude;
    private String locationLabel;
    private Boolean defaultAddress;
    private String fullAddress;
}
