package com.akgeneralstore.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OtpResponse {
    private boolean otpCreated;
    private String hint;
    private String maskedDestination;
}
