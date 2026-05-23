package com.akgeneralstore.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("prod")
public class ProductionReadinessValidator {

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.otp.from-email:}")
    private String otpFromEmail;

    @Value("${app.cors.allowed-origin-patterns:}")
    private String corsOrigins;

    @Value("${app.otp.brevo-api-key:}")
    private String brevoApiKey;

    @PostConstruct
    public void validate() {
        List<String> issues = new ArrayList<>();

        require(datasourceUrl, "spring.datasource.url", issues);
        require(datasourceUsername, "spring.datasource.username", issues);
        require(datasourcePassword, "spring.datasource.password", issues);
        require(jwtSecret, "jwt.secret", issues);
        require(otpFromEmail, "app.otp.from-email", issues);
        require(corsOrigins, "app.cors.allowed-origin-patterns", issues);

        boolean hasBrevoApi = brevoApiKey != null && !brevoApiKey.isBlank();
        boolean hasSmtp = mailUsername != null && !mailUsername.isBlank() && mailPassword != null && !mailPassword.isBlank();

        if (!hasBrevoApi && !hasSmtp) {
            issues.add("- Configure either BREVO_API_KEY for API-based OTP emails or SMTP credentials for email delivery.");
        }

        if (jwtSecret != null && jwtSecret.length() < 32) {
            issues.add("jwt.secret must be at least 32 characters long in production.");
        }

        if (!issues.isEmpty()) {
            throw new IllegalStateException(
                    "Production profile is not fully configured:" + System.lineSeparator() + String.join(System.lineSeparator(), issues)
            );
        }
    }

    private void require(String value, String key, List<String> issues) {
        if (value == null || value.isBlank()) {
            issues.add("- Missing required production property: " + key);
        }
    }
}
