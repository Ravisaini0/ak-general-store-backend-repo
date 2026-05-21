package com.akgeneralstore.controller;

import com.akgeneralstore.dto.request.ChakkiBookingRequest;
import com.akgeneralstore.dto.response.ApiResponse;
import com.akgeneralstore.dto.response.ChakkiBookingResponse;
import com.akgeneralstore.security.UserPrincipal;
import com.akgeneralstore.service.ChakkiBookingService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chakki-bookings")
public class ChakkiBookingController {

    private final ChakkiBookingService chakkiBookingService;

    public ChakkiBookingController(ChakkiBookingService chakkiBookingService) {
        this.chakkiBookingService = chakkiBookingService;
    }

    @PostMapping
    public ApiResponse<ChakkiBookingResponse> createBooking(
            @Valid @RequestBody ChakkiBookingRequest request,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new ApiResponse<>(true, "Booking created", chakkiBookingService.createBooking(principal.getId(), request));
    }

    @GetMapping("/my")
    public ApiResponse<List<ChakkiBookingResponse>> getMyBookings(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new ApiResponse<>(true, "Bookings fetched", chakkiBookingService.getUserBookings(principal.getId()));
    }
}
