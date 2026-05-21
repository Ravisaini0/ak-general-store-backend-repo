package com.akgeneralstore.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
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
}
