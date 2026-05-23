package com.akgeneralstore.service.impl;

import com.akgeneralstore.config.OtpDeliveryProperties;
import com.akgeneralstore.exception.BadRequestException;
import com.akgeneralstore.service.OtpDeliveryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class EmailOtpDeliveryService implements OtpDeliveryService {
    private static final String BREVO_SEND_EMAIL_URL = "https://api.brevo.com/v3/smtp/email";

    private final JavaMailSender mailSender;
    private final OtpDeliveryProperties otpDeliveryProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public EmailOtpDeliveryService(JavaMailSender mailSender, OtpDeliveryProperties otpDeliveryProperties, ObjectMapper objectMapper) {
        this.mailSender = mailSender;
        this.otpDeliveryProperties = otpDeliveryProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void sendOtp(String destination, String purpose, String code) {
        if (destination == null || destination.isBlank() || !destination.contains("@")) {
            throw new BadRequestException("A valid email destination is required for OTP delivery.");
        }

        if (otpDeliveryProperties.getFromEmail() == null || otpDeliveryProperties.getFromEmail().isBlank()) {
            throw new BadRequestException(
                    "OTP email delivery is not configured. Please connect SMTP credentials in the backend configuration."
            );
        }

        if (otpDeliveryProperties.getBrevoApiKey() != null && !otpDeliveryProperties.getBrevoApiKey().isBlank()) {
            sendViaBrevoApi(destination, purpose, code);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destination.trim().toLowerCase());
        message.setFrom(otpDeliveryProperties.getFromEmail().trim());
        message.setSubject(buildSubject(purpose));
        message.setText(buildBody(purpose, code));
        mailSender.send(message);
    }

    private void sendViaBrevoApi(String destination, String purpose, String code) {
        try {
            Map<String, Object> payload = Map.of(
                    "sender", Map.of(
                            "name", otpDeliveryProperties.getSenderName(),
                            "email", otpDeliveryProperties.getFromEmail().trim()
                    ),
                    "to", List.of(Map.of("email", destination.trim().toLowerCase())),
                    "subject", buildSubject(purpose),
                    "htmlContent", buildHtmlBody(purpose, code)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_SEND_EMAIL_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("api-key", otpDeliveryProperties.getBrevoApiKey().trim())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BadRequestException("OTP email delivery is temporarily unavailable. Please try again in a moment.");
            }
        } catch (BadRequestException exception) {
            throw exception;
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("OTP email delivery is temporarily unavailable. Please try again in a moment.");
        }
    }

    private String buildSubject(String purpose) {
        String normalizedPurpose = purpose == null ? "" : purpose.toUpperCase();
        if ("LOGIN".equals(normalizedPurpose)) {
            return otpDeliveryProperties.getSenderName() + " login verification code";
        }
        if ("RESET_PASSWORD".equals(normalizedPurpose)) {
            return otpDeliveryProperties.getSenderName() + " password reset code";
        }
        return otpDeliveryProperties.getSenderName() + " email verification code";
    }

    private String buildBody(String purpose, String code) {
        String normalizedPurpose = purpose == null ? "" : purpose.toUpperCase();
        String heading = "Use this code to verify your account:";
        if ("LOGIN".equals(normalizedPurpose)) {
            heading = "Use this code to complete your secure login:";
        } else if ("RESET_PASSWORD".equals(normalizedPurpose)) {
            heading = "Use this code to reset your password:";
        }

        return heading
                + System.lineSeparator()
                + System.lineSeparator()
                + code
                + System.lineSeparator()
                + System.lineSeparator()
                + "This code is valid for "
                + otpDeliveryProperties.getExpiryMinutes()
                + " minutes."
                + System.lineSeparator()
                + "If you did not request this code, please ignore this email.";
    }

    private String buildHtmlBody(String purpose, String code) {
        String normalizedPurpose = purpose == null ? "" : purpose.toUpperCase();
        String heading = "Use this code to verify your account";
        if ("LOGIN".equals(normalizedPurpose)) {
            heading = "Use this code to complete your secure login";
        } else if ("RESET_PASSWORD".equals(normalizedPurpose)) {
            heading = "Use this code to reset your password";
        }

        return "<html><body style=\"font-family:Arial,sans-serif;color:#0f172a;line-height:1.6;\">"
                + "<div style=\"max-width:480px;margin:0 auto;padding:24px;border:1px solid #e2e8f0;border-radius:18px;\">"
                + "<p style=\"font-size:12px;letter-spacing:0.22em;text-transform:uppercase;color:#f97316;font-weight:700;margin:0 0 12px;\">"
                + otpDeliveryProperties.getSenderName()
                + "</p>"
                + "<h2 style=\"margin:0 0 12px;font-size:24px;color:#020617;\">"
                + heading
                + "</h2>"
                + "<p style=\"margin:0 0 18px;color:#475569;\">Your one-time verification code is:</p>"
                + "<div style=\"font-size:32px;font-weight:800;letter-spacing:0.25em;background:#f8fafc;border:1px solid #e2e8f0;border-radius:14px;padding:18px;text-align:center;color:#0f172a;\">"
                + code
                + "</div>"
                + "<p style=\"margin:18px 0 0;color:#475569;\">This code is valid for "
                + otpDeliveryProperties.getExpiryMinutes()
                + " minutes.</p>"
                + "<p style=\"margin:8px 0 0;color:#94a3b8;font-size:13px;\">If you did not request this code, please ignore this email.</p>"
                + "</div></body></html>";
    }
}
