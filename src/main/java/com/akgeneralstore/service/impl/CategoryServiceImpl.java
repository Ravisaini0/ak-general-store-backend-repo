package com.akgeneralstore.service.impl;

import com.akgeneralstore.config.ProductUploadProperties;
import com.akgeneralstore.dto.response.ProductImageUploadResponse;
import com.akgeneralstore.entity.Category;
import com.akgeneralstore.exception.BadRequestException;
import com.akgeneralstore.exception.ResourceNotFoundException;
import com.akgeneralstore.repository.CategoryRepository;
import com.akgeneralstore.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CategoryServiceImpl implements CategoryService {
    private static final Path CATEGORY_UPLOAD_DIRECTORY = Paths.get("uploads", "categories");

    private final CategoryRepository categoryRepository;
    private final ProductUploadProperties productUploadProperties;

    public CategoryServiceImpl(
            CategoryRepository categoryRepository,
            ProductUploadProperties productUploadProperties
    ) {
        this.categoryRepository = categoryRepository;
        this.productUploadProperties = productUploadProperties;
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category createCategory(Category category) {
        if (category.getSlug() == null || category.getSlug().isBlank()) {
            category.setSlug(category.getName().trim().toLowerCase().replace(" ", "-"));
        }
        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(Long id, Category category) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        existing.setName(category.getName());
        existing.setSlug(category.getSlug());
        existing.setImageUrl(category.getImageUrl());
        existing.setActive(category.isActive());
        return categoryRepository.save(existing);
    }

    @Override
    public ProductImageUploadResponse uploadCategoryImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select an image file to upload.");
        }

        if (file.getSize() > productUploadProperties.getMaxBytes()) {
            throw new BadRequestException("Image size is too large. Please upload a smaller file.");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!productUploadProperties.getAllowedContentTypes().contains(contentType)) {
            throw new BadRequestException("Only JPG, PNG, or WEBP images are allowed.");
        }

        String originalName = sanitizeOriginalName(file.getOriginalFilename());
        String extension = resolveExtension(originalName);

        if (!productUploadProperties.getAllowedExtensions().contains(extension)) {
            throw new BadRequestException("Unsupported image extension. Allowed: JPG, PNG, WEBP.");
        }

        try {
            byte[] fileBytes = file.getBytes();
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(fileBytes));
            if (bufferedImage == null || bufferedImage.getWidth() <= 0 || bufferedImage.getHeight() <= 0) {
                throw new BadRequestException("Invalid image file uploaded.");
            }

            String fileName = UUID.randomUUID() + extension;
            Files.createDirectories(CATEGORY_UPLOAD_DIRECTORY);
            Path targetPath = CATEGORY_UPLOAD_DIRECTORY.resolve(fileName).normalize();

            if (!targetPath.startsWith(CATEGORY_UPLOAD_DIRECTORY.normalize())) {
                throw new BadRequestException("Invalid upload target path.");
            }

            Files.copy(new ByteArrayInputStream(fileBytes), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return new ProductImageUploadResponse("/uploads/categories/" + fileName);
        } catch (BadRequestException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BadRequestException("Category image could not be uploaded.");
        }
    }

    @Override
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found");
        }
        categoryRepository.deleteById(id);
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitizeOriginalName(String originalName) {
        String fallbackName = originalName == null || originalName.isBlank() ? "category-image.jpg" : originalName;
        String normalized = Normalizer.normalize(fallbackName, Normalizer.Form.NFKC)
                .replace("\\", "")
                .replace("/", "")
                .trim();

        if (normalized.isBlank()) {
            return "category-image.jpg";
        }

        return normalized;
    }

    private String resolveExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex >= fileName.length() - 1) {
            return ".jpg";
        }
        return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }
}
