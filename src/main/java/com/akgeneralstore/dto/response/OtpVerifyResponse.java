package com.akgeneralstore.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OtpVerifyResponse {
    private boolean verified;
    private String verificationToken;
    private String message;
}
