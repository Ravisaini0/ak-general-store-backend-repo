package com.akgeneralstore.controller;

import com.akgeneralstore.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ApiResponse<Map<String, Object>> health() {
        return new ApiResponse<>(
                true,
                "Backend is active",
                Map.of(
                        "status", "UP",
                        "service", "ak-general-store-backend",
                        "timestamp", Instant.now().toString()
                )
        );
    }
}
