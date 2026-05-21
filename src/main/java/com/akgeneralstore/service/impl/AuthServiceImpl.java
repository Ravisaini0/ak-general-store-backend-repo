package com.akgeneralstore.service.impl;

import com.akgeneralstore.config.OtpDeliveryProperties;
import com.akgeneralstore.config.ProductUploadProperties;
import com.akgeneralstore.dto.request.ForgotPasswordRequest;
import com.akgeneralstore.dto.request.LoginRequest;
import com.akgeneralstore.dto.request.OtpRequest;
import com.akgeneralstore.dto.request.OtpVerifyRequest;
import com.akgeneralstore.dto.request.RegisterRequest;
import com.akgeneralstore.dto.request.ResetPasswordRequest;
import com.akgeneralstore.dto.response.AuthResponse;
import com.akgeneralstore.dto.response.ForgotPasswordResponse;
import com.akgeneralstore.dto.response.MessageResponse;
import com.akgeneralstore.dto.response.OtpResponse;
import com.akgeneralstore.dto.response.OtpVerifyResponse;
import com.akgeneralstore.dto.response.ProductImageUploadResponse;
import com.akgeneralstore.entity.OtpVerification;
import com.akgeneralstore.entity.User;
import com.akgeneralstore.exception.BadRequestException;
import com.akgeneralstore.exception.ResourceNotFoundException;
import com.akgeneralstore.exception.UnauthorizedException;
import com.akgeneralstore.repository.OtpVerificationRepository;
import com.akgeneralstore.repository.UserRepository;
import com.akgeneralstore.security.JwtService;
import com.akgeneralstore.service.AuthService;
import com.akgeneralstore.service.OtpDeliveryService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Path AVATAR_UPLOAD_DIRECTORY = Paths.get("uploads", "avatars");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpVerificationRepository otpVerificationRepository;
    private final OtpDeliveryService otpDeliveryService;
    private final OtpDeliveryProperties otpDeliveryProperties;
    private final ProductUploadProperties productUploadProperties;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OtpVerificationRepository otpVerificationRepository,
            OtpDeliveryService otpDeliveryService,
            OtpDeliveryProperties otpDeliveryProperties,
            ProductUploadProperties productUploadProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpVerificationRepository = otpVerificationRepository;
        this.otpDeliveryService = otpDeliveryService;
        this.otpDeliveryProperties = otpDeliveryProperties;
        this.productUploadProperties = productUploadProperties;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() != com.akgeneralstore.enums.UserRole.CUSTOMER) {
            throw new BadRequestException("Only customer accounts can register here.");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already registered");
        }

        if (request.getPhone() != null && !request.getPhone().isBlank() && userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new BadRequestException("Phone already registered");
        }

        if (request.getRole() == com.akgeneralstore.enums.UserRole.CUSTOMER) {
            String identifier = request.getEmail().trim().toLowerCase();

            if (request.getVerificationToken() == null || request.getVerificationToken().isBlank()) {
                throw new BadRequestException("OTP verification is required before registration.");
            }

            OtpVerification verification = otpVerificationRepository
                    .findByVerificationTokenAndIdentifierAndPurpose(
                            request.getVerificationToken(),
                            identifier,
                            "REGISTER"
                    )
                    .orElseThrow(() -> new BadRequestException("Invalid registration verification token."));

            if (verification.getVerifiedAt() == null || verification.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("The registration OTP has expired. Please request a new OTP.");
            }
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setEmailVerified(request.getRole() != com.akgeneralstore.enums.UserRole.CUSTOMER || request.getVerificationToken() != null);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser.getEmail(), savedUser.getRole().name());
        return mapAuthResponse(savedUser, token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String loginValue = request.getLoginValue();
        if (loginValue.isBlank()) {
            throw new UnauthorizedException("Email or phone is required");
        }

        User user = findUserByLoginValue(loginValue)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (user.isBlocked()) {
            throw new UnauthorizedException("This account has been blocked. Please contact the store admin.");
        }

        if (user.getRole() == com.akgeneralstore.enums.UserRole.CUSTOMER && !user.isEmailVerified()) {
            throw new UnauthorizedException("Please verify your email before logging in.");
        }

        if (user.getRole() == com.akgeneralstore.enums.UserRole.CUSTOMER) {
            String identifier = user.getEmail().trim().toLowerCase();
            if (request.getVerificationToken() == null || request.getVerificationToken().isBlank()) {
                throw new UnauthorizedException("Please verify the login OTP before logging in.");
            }

            OtpVerification verification = otpVerificationRepository
                    .findByVerificationTokenAndIdentifierAndPurpose(
                            request.getVerificationToken(),
                            identifier,
                            "LOGIN"
                    )
                    .orElseThrow(() -> new UnauthorizedException("Invalid login verification token."));

            if (verification.getVerifiedAt() == null || verification.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new UnauthorizedException("The login OTP has expired. Please request a new OTP.");
            }
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return mapAuthResponse(user, token);
    }

    @Override
    public OtpResponse requestLoginOtp(LoginRequest request) {
        String loginValue = request.getLoginValue();
        if (loginValue.isBlank()) {
            throw new UnauthorizedException("Email or phone is required");
        }

        User user = findUserByLoginValue(loginValue)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.getRole() != com.akgeneralstore.enums.UserRole.CUSTOMER) {
            throw new UnauthorizedException("OTP login is only available for customer accounts.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (!user.isEmailVerified()) {
            throw new UnauthorizedException("Please verify your email before logging in.");
        }

        return requestOtpForPurpose(user.getEmail().trim().toLowerCase(), "LOGIN", user.getEmail().trim().toLowerCase());
    }

    @Override
    public OtpResponse requestOtp(OtpRequest request) {
        String identifier = sanitizeIdentifier(request.getIdentifier());
        String purpose = sanitizePurpose(request.getPurpose());

        return requestOtpForPurpose(identifier, purpose, resolveOtpDeliveryDestination(identifier));
    }

    private OtpResponse requestOtpForPurpose(String identifier, String purpose, String deliveryDestination) {
        if (deliveryDestination == null || deliveryDestination.isBlank()) {
            throw new BadRequestException("A verified account email is required to receive OTP codes.");
        }

        otpVerificationRepository.findByIdentifierAndPurposeAndVerifiedAtIsNull(identifier, purpose).forEach(existing -> {
            existing.setExpiresAt(LocalDateTime.now());
            otpVerificationRepository.save(existing);
        });

        OtpVerification otpVerification = new OtpVerification();
        otpVerification.setIdentifier(identifier);
        otpVerification.setPurpose(purpose);
        otpVerification.setCode(generateOtp());
        otpVerification.setExpiresAt(LocalDateTime.now().plusMinutes(otpDeliveryProperties.getExpiryMinutes()));
        otpVerificationRepository.save(otpVerification);

        otpDeliveryService.sendOtp(deliveryDestination, purpose, otpVerification.getCode());

        return OtpResponse.builder()
                .otpCreated(true)
                .hint("A verification code has been sent to your registered email.")
                .maskedDestination(maskIdentifier(deliveryDestination))
                .build();
    }

    @Override
    public OtpVerifyResponse verifyOtp(OtpVerifyRequest request) {
        String identifier = sanitizeIdentifier(request.getIdentifier());
        String purpose = sanitizePurpose(request.getPurpose());

        OtpVerification otpVerification = otpVerificationRepository
                .findTopByIdentifierAndPurposeOrderByCreatedAtDesc(identifier, purpose)
                .orElseThrow(() -> new BadRequestException("OTP was not requested for this identifier."));

        if (otpVerification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        if (!otpVerification.getCode().equals(request.getCode().trim())) {
            throw new BadRequestException("Invalid OTP entered.");
        }

        otpVerification.setVerifiedAt(LocalDateTime.now());
        otpVerification.setVerificationToken(UUID.randomUUID().toString());
        otpVerificationRepository.save(otpVerification);

        return OtpVerifyResponse.builder()
                .verified(true)
                .verificationToken(otpVerification.getVerificationToken())
                .message("OTP verified successfully.")
                .build();
    }

    @Override
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String identifier = request.getIdentifier() == null ? "" : request.getIdentifier().trim();
        if (identifier.isBlank()) {
            throw new BadRequestException("Email or phone is required");
        }

        String sanitizedIdentifier = identifier.contains("@") ? identifier.toLowerCase() : identifier;
        Optional<User> userOptional = findUserByLoginValue(sanitizedIdentifier);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            requestOtpForPurpose(sanitizedIdentifier, "RESET_PASSWORD", resolveOtpDeliveryDestination(user));
            return ForgotPasswordResponse.builder()
                    .resetRequested(true)
                    .message("If an account exists for this email or phone, a password reset code has been sent.")
                    .maskedDestination(maskIdentifier(resolveOtpDeliveryDestination(user)))
                    .build();
        }

        return ForgotPasswordResponse.builder()
                .resetRequested(true)
                .message("If an account exists for this email or phone, a password reset code has been sent.")
                .maskedDestination(maskIdentifier(sanitizedIdentifier))
                .build();
    }

    @Override
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String identifier = sanitizeIdentifier(request.getIdentifier());

        if (request.getVerificationToken() == null || request.getVerificationToken().isBlank()) {
            throw new BadRequestException("Password reset verification is required.");
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters.");
        }

        OtpVerification verification = otpVerificationRepository
                .findByVerificationTokenAndIdentifierAndPurpose(
                        request.getVerificationToken(),
                        identifier,
                        "RESET_PASSWORD"
                )
                .orElseThrow(() -> new BadRequestException("Invalid password reset verification token."));

        if (verification.getVerifiedAt() == null || verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This password reset verification has expired.");
        }

        User user = findUserByLoginValue(identifier)
                .orElseThrow(() -> new BadRequestException("User not found for this reset request."));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        verification.setExpiresAt(LocalDateTime.now());
        otpVerificationRepository.save(verification);

        return new MessageResponse("Password reset successful.");
    }

    @Override
    public ProductImageUploadResponse uploadProfileAvatar(Long userId, MultipartFile file) {
        if (userId == null || userId <= 0) {
            throw new UnauthorizedException("You must be logged in to update your profile image.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found."));

        String existingAvatarUrl = user.getAvatarUrl();
        String avatarUrl = uploadValidatedAvatar(file);
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        deleteAvatarIfManaged(existingAvatarUrl);
        return new ProductImageUploadResponse(avatarUrl);
    }

    @Override
    public MessageResponse removeProfileAvatar(Long userId) {
        if (userId == null || userId <= 0) {
            throw new UnauthorizedException("You must be logged in to update your profile image.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found."));

        String existingAvatarUrl = user.getAvatarUrl();
        user.setAvatarUrl(null);
        userRepository.save(user);
        deleteAvatarIfManaged(existingAvatarUrl);
        return new MessageResponse("Profile image removed.");
    }

    private Optional<User> findUserByLoginValue(String loginValue) {
        if (loginValue.contains("@")) {
            return userRepository.findByEmail(loginValue.toLowerCase());
        }

        return userRepository.findByPhone(loginValue);
    }

    private String sanitizeIdentifier(String rawIdentifier) {
        String identifier = rawIdentifier == null ? "" : rawIdentifier.trim();
        if (identifier.isBlank()) {
            throw new BadRequestException("Email or phone is required.");
        }
        return identifier.contains("@") ? identifier.toLowerCase() : identifier;
    }

    private String sanitizePurpose(String rawPurpose) {
        String purpose = rawPurpose == null || rawPurpose.isBlank() ? "REGISTER" : rawPurpose.trim().toUpperCase();
        return purpose;
    }

    private String generateOtp() {
        int code = (int) (Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }

    private String resolveOtpDeliveryDestination(String identifier) {
        return findUserByLoginValue(identifier)
                .map(this::resolveOtpDeliveryDestination)
                .orElseGet(() -> identifier.contains("@") ? identifier.toLowerCase() : "");
    }

    private String resolveOtpDeliveryDestination(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BadRequestException("This account does not have a recovery email configured.");
        }

        return user.getEmail().trim().toLowerCase();
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "";
        }

        if (identifier.contains("@")) {
            String[] parts = identifier.split("@", 2);
            String localPart = parts[0];
            String domainPart = parts[1];

            if (localPart.length() <= 2) {
                return localPart.charAt(0) + "***@" + domainPart;
            }

            return localPart.substring(0, 2) + "***@" + domainPart;
        }

        if (identifier.length() <= 4) {
            return "***" + identifier;
        }

        return "******" + identifier.substring(identifier.length() - 4);
    }

    private String uploadValidatedAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select an image file to upload.");
        }

        if (file.getSize() > productUploadProperties.getMaxBytes()) {
            throw new BadRequestException("Image size is too large. Please upload a smaller file.");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!productUploadProperties.getAllowedContentTypes().contains(contentType)) {
            throw new BadRequestException("Only JPG, PNG, or WEBP images are allowed.");
        }

        String originalName = sanitizeOriginalName(file.getOriginalFilename());
        String extension = resolveExtension(originalName);
        if (!productUploadProperties.getAllowedExtensions().contains(extension)) {
            throw new BadRequestException("Unsupported image extension. Allowed: JPG, PNG, WEBP.");
        }

        try {
            byte[] fileBytes = file.getBytes();
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(fileBytes));
            if (bufferedImage == null || bufferedImage.getWidth() <= 0 || bufferedImage.getHeight() <= 0) {
                throw new BadRequestException("Invalid image file uploaded.");
            }

            String fileName = UUID.randomUUID() + extension;
            Files.createDirectories(AVATAR_UPLOAD_DIRECTORY);
            Path targetPath = AVATAR_UPLOAD_DIRECTORY.resolve(fileName).normalize();

            if (!targetPath.startsWith(AVATAR_UPLOAD_DIRECTORY.normalize())) {
                throw new BadRequestException("Invalid upload target path.");
            }

            Files.copy(new ByteArrayInputStream(fileBytes), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/avatars/" + fileName;
        } catch (BadRequestException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BadRequestException("Profile image could not be uploaded.");
        }
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitizeOriginalName(String originalName) {
        String fallbackName = originalName == null || originalName.isBlank() ? "profile-image.jpg" : originalName;
        String normalized = Normalizer.normalize(fallbackName, Normalizer.Form.NFKC)
                .replace("\\", "")
                .replace("/", "")
                .trim();

        if (normalized.isBlank()) {
            return "profile-image.jpg";
        }

        return normalized;
    }

    private String resolveExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex >= fileName.length() - 1) {
            return ".jpg";
        }
        return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private void deleteAvatarIfManaged(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank() || !avatarUrl.startsWith("/uploads/avatars/")) {
            return;
        }

        try {
            String fileName = avatarUrl.substring("/uploads/avatars/".length()).trim();
            if (fileName.isBlank() || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                return;
            }

            Path targetPath = AVATAR_UPLOAD_DIRECTORY.resolve(fileName).normalize();
            if (targetPath.startsWith(AVATAR_UPLOAD_DIRECTORY.normalize())) {
                Files.deleteIfExists(targetPath);
            }
        } catch (IOException ignored) {
            // Keep the profile update successful even if cleanup could not be completed.
        }
    }

    private AuthResponse mapAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .role(user.getRole())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatarUrl())
                .emailVerified(user.isEmailVerified())
                .blocked(user.isBlocked())
                .build();
    }
}
