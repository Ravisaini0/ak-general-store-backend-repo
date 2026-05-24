package com.akgeneralstore.controller;

import com.akgeneralstore.entity.StoredAsset;
import com.akgeneralstore.exception.ResourceNotFoundException;
import com.akgeneralstore.service.AssetStorageService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
public class AssetController {

    private final AssetStorageService assetStorageService;

    public AssetController(AssetStorageService assetStorageService) {
        this.assetStorageService = assetStorageService;
    }

    @GetMapping("/api/assets/{token}")
    public ResponseEntity<byte[]> getAsset(@PathVariable String token) {
        StoredAsset asset = assetStorageService.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            mediaType = MediaType.parseMediaType(asset.getContentType());
        } catch (IllegalArgumentException ignored) {
            // Keep the asset downloadable even if the content type string is malformed.
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + asset.getFileName() + "\"")
                .contentType(mediaType)
                .body(asset.getData());
    }
}
