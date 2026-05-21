package com.akgeneralstore.dto.request;

import com.akgeneralstore.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    private String phone;
    private String verificationToken;
    private UserRole role = UserRole.CUSTOMER;
}
