package com.akgeneralstore.entity;

import com.akgeneralstore.enums.DeliveryBoyStatus;
import com.akgeneralstore.enums.UserRole;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phone;

    private boolean emailVerified;

    @Column(nullable = false)
    private boolean blocked = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.CUSTOMER;

    @Enumerated(EnumType.STRING)
    private DeliveryBoyStatus deliveryStatus;
}
