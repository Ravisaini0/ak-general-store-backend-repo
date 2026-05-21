package com.akgeneralstore.service.impl;

import com.akgeneralstore.dto.request.ChakkiBookingRequest;
import com.akgeneralstore.dto.response.ChakkiBookingResponse;
import com.akgeneralstore.entity.ChakkiBooking;
import com.akgeneralstore.repository.ChakkiBookingRepository;
import com.akgeneralstore.service.ChakkiBookingService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChakkiBookingServiceImpl implements ChakkiBookingService {

    private final ChakkiBookingRepository chakkiBookingRepository;

    public ChakkiBookingServiceImpl(ChakkiBookingRepository chakkiBookingRepository) {
        this.chakkiBookingRepository = chakkiBookingRepository;
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

    private ChakkiBookingResponse mapBooking(ChakkiBooking booking) {
        return ChakkiBookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .fullName(booking.getFullName())
                .phone(booking.getPhone())
                .pickupAddress(booking.getPickupAddress())
                .grainType(booking.getGrainType())
                .quantityKg(booking.getQuantityKg())
                .preferredSlot(booking.getPreferredSlot())
                .notes(booking.getNotes())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
