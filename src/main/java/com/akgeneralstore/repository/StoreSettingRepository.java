package com.akgeneralstore.repository;

import com.akgeneralstore.entity.StoreSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreSettingRepository extends JpaRepository<StoreSetting, Long> {
    Optional<StoreSetting> findBySettingKey(String settingKey);
}
