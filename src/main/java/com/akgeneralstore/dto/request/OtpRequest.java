package com.akgeneralstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpRequest {

    @NotBlank
    private String identifier;

    private String purpose = "REGISTER";
}
