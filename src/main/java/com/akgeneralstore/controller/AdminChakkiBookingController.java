package com.akgeneralstore.controller;

import com.akgeneralstore.dto.response.ApiResponse;
import com.akgeneralstore.dto.response.ChakkiBookingResponse;
import com.akgeneralstore.service.ChakkiBookingService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/chakki-bookings")
public class AdminChakkiBookingController {

    private final ChakkiBookingService chakkiBookingService;

    public AdminChakkiBookingController(ChakkiBookingService chakkiBookingService) {
        this.chakkiBookingService = chakkiBookingService;
    }

    @GetMapping
    public ApiResponse<List<ChakkiBookingResponse>> getAllBookings() {
        return new ApiResponse<>(true, "Chakki bookings fetched", chakkiBookingService.getAllBookings());
    }

    @PutMapping("/{bookingId}/status")
    public ApiResponse<ChakkiBookingResponse> updateStatus(
            @PathVariable Long bookingId,
            @RequestParam String status
    ) {
        return new ApiResponse<>(
                true,
                "Chakki booking status updated",
                chakkiBookingService.updateBookingStatus(bookingId, status)
        );
    }
}
