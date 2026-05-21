package com.akgeneralstore.repository;

import com.akgeneralstore.entity.ChakkiBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChakkiBookingRepository extends JpaRepository<ChakkiBooking, Long> {
    List<ChakkiBooking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ChakkiBooking> findAllByOrderByCreatedAtDesc();
}
