package com.akgeneralstore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.otp")
public class OtpDeliveryProperties {
    private String senderName = "AK General Store";
    private String fromEmail = "";
    private int expiryMinutes = 10;
}
