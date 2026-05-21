package com.akgeneralstore.service.impl;

import com.akgeneralstore.dto.request.ChakkiBookingRequest;
import com.akgeneralstore.dto.response.ChakkiBookingResponse;
import com.akgeneralstore.entity.ChakkiBooking;
import com.akgeneralstore.entity.User;
import com.akgeneralstore.exception.BadRequestException;
import com.akgeneralstore.exception.ResourceNotFoundException;
import com.akgeneralstore.repository.ChakkiBookingRepository;
import com.akgeneralstore.repository.UserRepository;
import com.akgeneralstore.service.ChakkiBookingService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ChakkiBookingServiceImpl implements ChakkiBookingService {
    private static final Set<String> ALLOWED_STATUSES = new LinkedHashSet<>(Arrays.asList(
            "BOOKED",
            "PICKUP_CONFIRMED",
            "IN_MILLING",
            "READY_FOR_DELIVERY",
            "COMPLETED",
            "CANCELLED"
    ));

    private final ChakkiBookingRepository chakkiBookingRepository;
    private final UserRepository userRepository;

    public ChakkiBookingServiceImpl(
            ChakkiBookingRepository chakkiBookingRepository,
            UserRepository userRepository
    ) {
        this.chakkiBookingRepository = chakkiBookingRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ChakkiBookingResponse createBooking(Long userId, ChakkiBookingRequest request) {
        ChakkiBooking booking = new ChakkiBooking();
        booking.setUserId(userId);
        booking.setFullName(request.getFullName());
        booking.setPhone(request.getPhone());
        booking.setPickupAddress(request.getPickupAddress());
        booking.setGrainType(request.getGrainType());
        booking.setQuantityKg(request.getQuantityKg());
        booking.setPreferredSlot(request.getPreferredSlot());
        booking.setNotes(request.getNotes());
        booking.setStatus("BOOKED");
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        return mapBooking(chakkiBookingRepository.save(booking));
    }

    @Override
    public List<ChakkiBookingResponse> getUserBookings(Long userId) {
        return chakkiBookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::mapBooking).toList();
    }

    @Override
    public List<ChakkiBookingResponse> getAllBookings() {
        return chakkiBookingRepository.findAllByOrderByCreatedAtDesc().stream().map(this::mapBooking).toList();
    }

    @Override
    public ChakkiBookingResponse updateBookingStatus(Long bookingId, String status) {
        String normalizedStatus = normalizeStatus(status);
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new BadRequestException("Unsupported chakki booking status.");
        }

        ChakkiBooking booking = chakkiBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Chakki booking not found."));

        booking.setStatus(normalizedStatus);
        booking.setUpdatedAt(LocalDateTime.now());
        return mapBooking(chakkiBookingRepository.save(booking));
    }

    private ChakkiBookingResponse mapBooking(ChakkiBooking booking) {
        User customer = userRepository.findById(booking.getUserId()).orElse(null);

        return ChakkiBookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .customerName(customer != null ? customer.getName() : booking.getFullName())
                .customerEmail(customer != null ? customer.getEmail() : null)
                .fullName(booking.getFullName())
                .phone(booking.getPhone())
                .pickupAddress(booking.getPickupAddress())
                .grainType(booking.getGrainType())
                .quantityKg(booking.getQuantityKg())
                .preferredSlot(booking.getPreferredSlot())
                .notes(booking.getNotes())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }
}
