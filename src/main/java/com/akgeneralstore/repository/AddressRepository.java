package com.akgeneralstore.repository;

import com.akgeneralstore.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdOrderByIdDesc(Long userId);
    Optional<Address> findByIdAndUserId(Long id, Long userId);
    List<Address> findByUserId(Long userId);
}
