package com.akgeneralstore.controller;

import com.akgeneralstore.dto.request.ForgotPasswordRequest;
import com.akgeneralstore.dto.request.LoginRequest;
import com.akgeneralstore.dto.request.OtpRequest;
import com.akgeneralstore.dto.request.OtpVerifyRequest;
import com.akgeneralstore.dto.request.ResetPasswordRequest;
import com.akgeneralstore.dto.request.RegisterRequest;
import com.akgeneralstore.dto.response.ApiResponse;
import com.akgeneralstore.dto.response.AuthResponse;
import com.akgeneralstore.dto.response.ForgotPasswordResponse;
import com.akgeneralstore.dto.response.MessageResponse;
import com.akgeneralstore.dto.response.OtpResponse;
import com.akgeneralstore.dto.response.OtpVerifyResponse;
import com.akgeneralstore.dto.response.ProductImageUploadResponse;
import com.akgeneralstore.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.akgeneralstore.security.UserPrincipal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return new ApiResponse<>(true, "Registration successful", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return new ApiResponse<>(true, "Login successful", authService.login(request));
    }

    @PostMapping("/request-login-otp")
    public ApiResponse<OtpResponse> requestLoginOtp(@Valid @RequestBody LoginRequest request) {
        return new ApiResponse<>(true, "Login OTP sent", authService.requestLoginOtp(request));
    }

    @PostMapping("/request-otp")
    public ApiResponse<OtpResponse> requestOtp(@Valid @RequestBody OtpRequest request) {
        return new ApiResponse<>(true, "OTP sent", authService.requestOtp(request));
    }

    @PostMapping("/verify-otp")
    public ApiResponse<OtpVerifyResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return new ApiResponse<>(true, "OTP verified", authService.verifyOtp(request));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<ForgotPasswordResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return new ApiResponse<>(true, "Password reset request processed", authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ApiResponse<MessageResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        return new ApiResponse<>(true, "Password reset successful", authService.resetPassword(request));
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProductImageUploadResponse> uploadProfileAvatar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file
    ) {
        return new ApiResponse<>(true, "Profile image updated", authService.uploadProfileAvatar(principal.getId(), file));
    }

    @DeleteMapping("/profile/avatar")
    public ApiResponse<MessageResponse> removeProfileAvatar(@AuthenticationPrincipal UserPrincipal principal) {
        return new ApiResponse<>(true, "Profile image removed", authService.removeProfileAvatar(principal.getId()));
    }
}
