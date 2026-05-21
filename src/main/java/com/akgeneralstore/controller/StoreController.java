package com.akgeneralstore.controller;

import com.akgeneralstore.dto.response.ApiResponse;
import com.akgeneralstore.dto.response.StoreSettingsResponse;
import com.akgeneralstore.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StoreController {

    private final AdminService adminService;

    public StoreController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/api/store/settings")
    public ApiResponse<StoreSettingsResponse> getPublicStoreSettings() {
        return new ApiResponse<>(true, "Store settings fetched", adminService.getSettings());
    }
}
