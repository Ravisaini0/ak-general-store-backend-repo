package com.akgeneralstore.repository;

import com.akgeneralstore.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    List<OtpVerification> findByIdentifierAndPurposeAndVerifiedAtIsNull(String identifier, String purpose);
    Optional<OtpVerification> findTopByIdentifierAndPurposeOrderByCreatedAtDesc(String identifier, String purpose);
    Optional<OtpVerification> findByVerificationTokenAndIdentifierAndPurpose(
            String verificationToken,
            String identifier,
            String purpose
    );
}
