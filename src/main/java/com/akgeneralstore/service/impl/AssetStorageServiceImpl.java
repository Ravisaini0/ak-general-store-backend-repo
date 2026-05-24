package com.akgeneralstore.service.impl;

import com.akgeneralstore.config.ProductUploadProperties;
import com.akgeneralstore.entity.StoredAsset;
import com.akgeneralstore.exception.BadRequestException;
import com.akgeneralstore.repository.StoredAssetRepository;
import com.akgeneralstore.service.AssetStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssetStorageServiceImpl implements AssetStorageService {
    private static final Path UPLOADS_DIRECTORY = Paths.get("uploads").toAbsolutePath().normalize();
    private static final String ASSET_URL_PREFIX = "/api/assets/";

    private final StoredAssetRepository storedAssetRepository;
    private final ProductUploadProperties productUploadProperties;

    public AssetStorageServiceImpl(
            StoredAssetRepository storedAssetRepository,
            ProductUploadProperties productUploadProperties
    ) {
        this.storedAssetRepository = storedAssetRepository;
        this.productUploadProperties = productUploadProperties;
    }

    @Override
    public String storeImage(MultipartFile file, String scope, String fallbackFileName) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select an image file to upload.");
        }

        if (file.getSize() > productUploadProperties.getMaxBytes()) {
            throw new BadRequestException(String.format(
                    "Image size is too large. Max allowed size is %s. Please compress or resize the image and try again.",
                    formatBytes(productUploadProperties.getMaxBytes())
            ));
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!productUploadProperties.getAllowedContentTypes().contains(contentType)) {
            throw new BadRequestException(String.format(
                    "Unsupported image type%s. Please upload a JPG, PNG, or WEBP image.",
                    contentType.isBlank() ? "" : " (" + contentType + ")"
            ));
        }

        String originalName = sanitizeOriginalName(file.getOriginalFilename(), fallbackFileName);
        String extension = resolveExtension(originalName);
        if (!productUploadProperties.getAllowedExtensions().contains(extension)) {
            throw new BadRequestException(String.format(
                    "Unsupported file extension %s. Please convert the image to JPG, PNG, or WEBP.",
                    extension
            ));
        }

        try {
            byte[] fileBytes = file.getBytes();
            validateImageBytes(fileBytes);
            return createAsset(scope, originalName, contentType, fileBytes);
        } catch (BadRequestException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BadRequestException("Image could not be uploaded because the file could not be read. Please try a fresh copy of the image.");
        }
    }

    @Override
    public String normalizeAssetUrl(String assetUrl, String scope) {
        if (assetUrl == null || assetUrl.isBlank()) {
            return assetUrl;
        }

        String trimmedUrl = assetUrl.trim();
        if (trimmedUrl.startsWith(ASSET_URL_PREFIX)) {
            return trimmedUrl;
        }

        String path = extractPath(trimmedUrl);
        if (path != null && path.startsWith(ASSET_URL_PREFIX)) {
            return path;
        }

        if (path == null || !path.startsWith("/uploads/")) {
            return trimmedUrl;
        }

        return migrateLegacyUpload(trimmedUrl, scope).orElse(trimmedUrl);
    }

    @Override
    public void deleteManagedAsset(String assetUrl) {
        extractAssetToken(assetUrl).flatMap(storedAssetRepository::findByToken).ifPresent(storedAssetRepository::delete);
    }

    @Override
    public Optional<StoredAsset> findByToken(String token) {
        return storedAssetRepository.findByToken(token);
    }

    private Optional<String> migrateLegacyUpload(String assetUrl, String scope) {
        String path = extractPath(assetUrl);
        if (path == null || !path.startsWith("/uploads/")) {
            return Optional.empty();
        }

        try {
            Path relativePath = Paths.get(path.substring("/uploads/".length())).normalize();
            Path sourcePath = UPLOADS_DIRECTORY.resolve(relativePath).normalize();
            if (!sourcePath.startsWith(UPLOADS_DIRECTORY) || !Files.exists(sourcePath) || !Files.isRegularFile(sourcePath)) {
                return Optional.empty();
            }

            byte[] fileBytes = Files.readAllBytes(sourcePath);
            validateImageBytes(fileBytes);
            String fileName = sourcePath.getFileName() == null ? "image.jpg" : sourcePath.getFileName().toString();
            String contentType = probeContentType(fileName, sourcePath);
            String newUrl = createAsset(scope, fileName, contentType, fileBytes);
            try {
                Files.deleteIfExists(sourcePath);
            } catch (IOException ignored) {
                // Legacy cleanup failure should never block migration.
            }
            return Optional.of(newUrl);
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private String createAsset(String scope, String originalName, String contentType, byte[] fileBytes) {
        StoredAsset storedAsset = new StoredAsset();
        storedAsset.setToken(UUID.randomUUID().toString().replace("-", ""));
        storedAsset.setFileName(originalName);
        storedAsset.setContentType(contentType);
        storedAsset.setScope(scope == null || scope.isBlank() ? "generic" : scope.trim().toLowerCase(Locale.ROOT));
        storedAsset.setData(fileBytes);
        storedAsset.setCreatedAt(LocalDateTime.now());
        storedAssetRepository.save(storedAsset);
        return ASSET_URL_PREFIX + storedAsset.getToken();
    }

    private void validateImageBytes(byte[] fileBytes) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(fileBytes));
        if (bufferedImage == null || bufferedImage.getWidth() <= 0 || bufferedImage.getHeight() <= 0) {
            throw new BadRequestException("This file is not a readable image. Please re-export it as JPG, PNG, or WEBP and try again.");
        }
    }

    private String formatBytes(long bytes) {
        double megabytes = bytes / (1024d * 1024d);
        if (megabytes >= 10) {
            return Math.round(megabytes) + " MB";
        }
        return String.format(Locale.ROOT, "%.1f MB", megabytes);
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitizeOriginalName(String originalName, String fallbackFileName) {
        String candidate = originalName == null || originalName.isBlank() ? fallbackFileName : originalName;
        String normalized = Normalizer.normalize(candidate, Normalizer.Form.NFKC)
                .replace("\\", "")
                .replace("/", "")
                .trim();

        return normalized.isBlank() ? fallbackFileName : normalized;
    }

    private String resolveExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex >= fileName.length() - 1) {
            return ".jpg";
        }
        return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private String extractPath(String assetUrl) {
        if (assetUrl == null || assetUrl.isBlank()) {
            return null;
        }

        if (assetUrl.startsWith("/")) {
            return assetUrl.trim();
        }

        try {
            return new URI(assetUrl.trim()).getPath();
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private Optional<String> extractAssetToken(String assetUrl) {
        String path = extractPath(assetUrl);
        if (path == null || !path.startsWith(ASSET_URL_PREFIX)) {
            return Optional.empty();
        }

        String token = path.substring(ASSET_URL_PREFIX.length()).trim();
        return token.isBlank() ? Optional.empty() : Optional.of(token);
    }

    private String probeContentType(String fileName, Path sourcePath) {
        try {
            String detected = Files.probeContentType(sourcePath);
            if (detected != null && !detected.isBlank()) {
                return detected.toLowerCase(Locale.ROOT);
            }
        } catch (IOException ignored) {
            // Fall back to the extension-based detection below.
        }

        String extension = resolveExtension(fileName);
        switch (extension) {
            case ".png":
                return "image/png";
            case ".webp":
                return "image/webp";
            default:
                return "image/jpeg";
        }
    }
}
