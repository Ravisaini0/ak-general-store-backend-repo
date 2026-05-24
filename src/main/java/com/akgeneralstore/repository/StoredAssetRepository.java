package com.akgeneralstore.repository;

import com.akgeneralstore.entity.StoredAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoredAssetRepository extends JpaRepository<StoredAsset, Long> {
    Optional<StoredAsset> findByToken(String token);
}
