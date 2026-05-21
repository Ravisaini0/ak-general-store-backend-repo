package com.akgeneralstore.controller;

import com.akgeneralstore.dto.request.AddressRequest;
import com.akgeneralstore.dto.response.AddressResponse;
import com.akgeneralstore.dto.response.ApiResponse;
import com.akgeneralstore.security.UserPrincipal;
import com.akgeneralstore.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ApiResponse<List<AddressResponse>> getAddresses(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new ApiResponse<>(true, "Addresses fetched", addressService.getAddresses(principal.getId()));
    }

    @PostMapping
    public ApiResponse<AddressResponse> createAddress(@Valid @RequestBody AddressRequest request, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new ApiResponse<>(true, "Address created", addressService.createAddress(principal.getId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressResponse> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new ApiResponse<>(true, "Address updated", addressService.updateAddress(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAddress(@PathVariable Long id, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        addressService.deleteAddress(principal.getId(), id);
        return new ApiResponse<>(true, "Address deleted", null);
    }
}
