package com.akgeneralstore.service;

import com.akgeneralstore.dto.request.ChakkiBookingRequest;
import com.akgeneralstore.dto.response.ChakkiBookingResponse;

import java.util.List;

public interface ChakkiBookingService {
    ChakkiBookingResponse createBooking(Long userId, ChakkiBookingRequest request);
    List<ChakkiBookingResponse> getUserBookings(Long userId);
    List<ChakkiBookingResponse> getAllBookings();
}
