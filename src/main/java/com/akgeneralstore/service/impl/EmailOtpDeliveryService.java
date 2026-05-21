package com.akgeneralstore.service.impl;

import com.akgeneralstore.config.OtpDeliveryProperties;
import com.akgeneralstore.exception.BadRequestException;
import com.akgeneralstore.service.OtpDeliveryService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailOtpDeliveryService implements OtpDeliveryService {

    private final JavaMailSender mailSender;
    private final OtpDeliveryProperties otpDeliveryProperties;

    public EmailOtpDeliveryService(JavaMailSender mailSender, OtpDeliveryProperties otpDeliveryProperties) {
        this.mailSender = mailSender;
        this.otpDeliveryProperties = otpDeliveryProperties;
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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destination.trim().toLowerCase());
        message.setFrom(otpDeliveryProperties.getFromEmail().trim());
        message.setSubject(buildSubject(purpose));
        message.setText(buildBody(purpose, code));
        mailSender.send(message);
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
}
