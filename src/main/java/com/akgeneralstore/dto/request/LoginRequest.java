package com.akgeneralstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    private String email;
    private String phone;
    private String identifier;
    private String verificationToken;

    @NotBlank
    private String password;

    public String getLoginValue() {
        if (identifier != null && !identifier.isBlank()) {
            return identifier.trim();
        }

        if (email != null && !email.isBlank()) {
            return email.trim();
        }

        if (phone != null && !phone.isBlank()) {
            return phone.trim();
        }

        return "";
    }
}
