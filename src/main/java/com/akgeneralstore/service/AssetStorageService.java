package com.akgeneralstore.service;

import com.akgeneralstore.entity.StoredAsset;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface AssetStorageService {
    String storeImage(MultipartFile file, String scope, String fallbackFileName);
    String normalizeAssetUrl(String assetUrl, String scope);
    void deleteManagedAsset(String assetUrl);
    Optional<StoredAsset> findByToken(String token);
}
