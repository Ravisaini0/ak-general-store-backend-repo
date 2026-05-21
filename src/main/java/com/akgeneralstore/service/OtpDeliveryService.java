package com.akgeneralstore.service;

public interface OtpDeliveryService {
    void sendOtp(String destination, String purpose, String code);
}
