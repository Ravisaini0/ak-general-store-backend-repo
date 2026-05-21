package com.akgeneralstore.service.impl;

import com.akgeneralstore.dto.request.AddressRequest;
import com.akgeneralstore.dto.response.AddressResponse;
import com.akgeneralstore.entity.Address;
import com.akgeneralstore.exception.ResourceNotFoundException;
import com.akgeneralstore.repository.AddressRepository;
import com.akgeneralstore.service.AddressService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;

    public AddressServiceImpl(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    public List<AddressResponse> getAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIdDesc(userId).stream()
                .sorted((left, right) -> Boolean.TRUE.equals(right.getDefaultAddress())
                        ? 1
                        : Boolean.TRUE.equals(left.getDefaultAddress()) ? -1 : 0)
                .map(this::mapAddress)
                .toList();
    }

    @Override
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        Address address = new Address();
        address.setUserId(userId);
        applyRequest(address, request);
        normalizeDefaultAddress(userId, address, request.getDefaultAddress());
        return mapAddress(addressRepository.save(address));
    }

    @Override
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        applyRequest(address, request);
        normalizeDefaultAddress(userId, address, request.getDefaultAddress());
        return mapAddress(addressRepository.save(address));
    }

    @Override
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        boolean wasDefault = Boolean.TRUE.equals(address.getDefaultAddress());
        addressRepository.delete(address);

        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserIdOrderByIdDesc(userId);
            if (!remaining.isEmpty()) {
                Address nextDefault = remaining.get(0);
                nextDefault.setDefaultAddress(true);
                addressRepository.save(nextDefault);
            }
        }
    }

    private void applyRequest(Address address, AddressRequest request) {
        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setAddressType(request.getAddressType());
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setLandmark(request.getLandmark());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setLocationLabel(request.getLocationLabel());
        address.setDefaultAddress(Boolean.TRUE.equals(request.getDefaultAddress()));
    }

    private void normalizeDefaultAddress(Long userId, Address currentAddress, Boolean defaultRequested) {
        boolean makeDefault = Boolean.TRUE.equals(defaultRequested);
        List<Address> addresses = addressRepository.findByUserId(userId);

        if (!makeDefault && addresses.isEmpty()) {
            makeDefault = true;
        }

        if (makeDefault) {
            addresses.stream()
                    .filter(address -> !Objects.equals(address.getId(), currentAddress.getId()))
                    .filter(address -> Boolean.TRUE.equals(address.getDefaultAddress()))
                    .forEach(address -> address.setDefaultAddress(false));
            addressRepository.saveAll(addresses);
            currentAddress.setDefaultAddress(true);
            return;
        }

        boolean hasOtherDefault = addresses.stream()
                .filter(address -> !Objects.equals(address.getId(), currentAddress.getId()))
                .anyMatch(address -> Boolean.TRUE.equals(address.getDefaultAddress()));

        currentAddress.setDefaultAddress(!hasOtherDefault);
    }

    private AddressResponse mapAddress(Address address) {
        String fullAddress = String.join(", ",
                address.getLine1(),
                address.getLine2() == null || address.getLine2().isBlank() ? "" : address.getLine2(),
                address.getCity(),
                address.getState(),
                address.getPincode()
        ).replaceAll(",\\s*,", ",").replaceAll(", $", "");

        return AddressResponse.builder()
                .id(address.getId())
                .fullName(address.getFullName())
                .phone(address.getPhone())
                .addressType(address.getAddressType())
                .line1(address.getLine1())
                .line2(address.getLine2())
                .city(address.getCity())
                .state(address.getState())
                .pincode(address.getPincode())
                .landmark(address.getLandmark())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .locationLabel(address.getLocationLabel())
                .defaultAddress(Boolean.TRUE.equals(address.getDefaultAddress()))
                .fullAddress(fullAddress)
                .build();
    }
}
