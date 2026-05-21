package com.akgeneralstore.dto.request;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String identifier;
    private String verificationToken;
    private String newPassword;
}
