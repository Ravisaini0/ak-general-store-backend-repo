package com.akgeneralstore.controller;

import com.akgeneralstore.dto.response.ApiResponse;
import com.akgeneralstore.dto.response.ChakkiBookingResponse;
import com.akgeneralstore.service.ChakkiBookingService;
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
}
