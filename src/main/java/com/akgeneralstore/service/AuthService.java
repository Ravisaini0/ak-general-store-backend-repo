package com.akgeneralstore.service;

import com.akgeneralstore.dto.request.ForgotPasswordRequest;
import com.akgeneralstore.dto.request.LoginRequest;
import com.akgeneralstore.dto.request.OtpRequest;
import com.akgeneralstore.dto.request.OtpVerifyRequest;
import com.akgeneralstore.dto.request.ResetPasswordRequest;
import com.akgeneralstore.dto.request.RegisterRequest;
import com.akgeneralstore.dto.response.AuthResponse;
import com.akgeneralstore.dto.response.ForgotPasswordResponse;
import com.akgeneralstore.dto.response.MessageResponse;
import com.akgeneralstore.dto.response.OtpResponse;
import com.akgeneralstore.dto.response.OtpVerifyResponse;
import com.akgeneralstore.dto.response.ProductImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    OtpResponse requestLoginOtp(LoginRequest request);
    OtpResponse requestOtp(OtpRequest request);
    OtpVerifyResponse verifyOtp(OtpVerifyRequest request);
    ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request);
    MessageResponse resetPassword(ResetPasswordRequest request);
    ProductImageUploadResponse uploadProfileAvatar(Long userId, MultipartFile file);
    MessageResponse removeProfileAvatar(Long userId);
}
