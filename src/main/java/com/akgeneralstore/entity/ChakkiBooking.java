package com.akgeneralstore.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chakki_bookings")
public class ChakkiBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false, length = 1000)
    private String pickupAddress;

    @Column(nullable = false)
    private String grainType;

    @Column(nullable = false)
    private Integer quantityKg;

    private String preferredSlot;

    @Column(length = 1200)
    private String notes;

    @Column(nullable = false)
    private String status = "BOOKED";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
