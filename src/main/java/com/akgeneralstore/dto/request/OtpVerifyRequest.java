package com.akgeneralstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpVerifyRequest {

    @NotBlank
    private String identifier;

    @NotBlank
    private String code;

    private String purpose = "REGISTER";
}
