package com.akgeneralstore.service;

import com.akgeneralstore.dto.request.AddressRequest;
import com.akgeneralstore.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {
    List<AddressResponse> getAddresses(Long userId);
    AddressResponse createAddress(Long userId, AddressRequest request);
    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request);
    void deleteAddress(Long userId, Long addressId);
}
