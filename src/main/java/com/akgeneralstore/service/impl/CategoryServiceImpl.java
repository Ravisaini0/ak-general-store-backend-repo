package com.akgeneralstore.service.impl;

import com.akgeneralstore.dto.response.CategoryBulkImportResponse;
import com.akgeneralstore.dto.response.ProductImageUploadResponse;
import com.akgeneralstore.entity.Category;
import com.akgeneralstore.exception.BadRequestException;
import com.akgeneralstore.exception.ResourceNotFoundException;
import com.akgeneralstore.repository.CategoryRepository;
import com.akgeneralstore.service.AssetStorageService;
import com.akgeneralstore.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final AssetStorageService assetStorageService;

    public CategoryServiceImpl(
            CategoryRepository categoryRepository,
            AssetStorageService assetStorageService
    ) {
        this.categoryRepository = categoryRepository;
        this.assetStorageService = assetStorageService;
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll().stream().map(this::normalizeCategoryImage).toList();
    }

    @Override
    public Category createCategory(Category category) {
        if (category.getSlug() == null || category.getSlug().isBlank()) {
            category.setSlug(buildSlug(category.getName()));
        }
        category.setImageUrl(assetStorageService.normalizeAssetUrl(category.getImageUrl(), "category"));
        return categoryRepository.save(category);
    }

    @Override
    public CategoryBulkImportResponse bulkImportCategories(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            throw new BadRequestException("Please upload at least one category row.");
        }

        List<Category> importedCategories = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int createdCount = 0;
        int updatedCount = 0;

        for (int index = 0; index < categories.size(); index++) {
            Category category = categories.get(index);
            int rowNumber = index + 1;

            try {
                validateBulkCategoryRow(category);
                String slug = category.getSlug() == null || category.getSlug().isBlank()
                        ? buildSlug(category.getName())
                        : buildSlug(category.getSlug());
                Category existing = categoryRepository.findBySlug(slug).orElseGet(Category::new);
                boolean isNewCategory = existing.getId() == null;

                existing.setName(category.getName().trim());
                existing.setSlug(slug);
                existing.setImageUrl(assetStorageService.normalizeAssetUrl(category.getImageUrl(), "category"));
                existing.setActive(category.isActive());

                importedCategories.add(categoryRepository.save(existing));

                if (isNewCategory) {
                    createdCount++;
                } else {
                    updatedCount++;
                }
            } catch (Exception exception) {
                errors.add("Row " + rowNumber + ": " + exception.getMessage());
            }
        }

        return CategoryBulkImportResponse.builder()
                .totalRows(categories.size())
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .failedCount(errors.size())
                .categories(importedCategories)
                .errors(errors)
                .build();
    }

    @Override
    public Category updateCategory(Long id, Category category) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        String previousImageUrl = existing.getImageUrl();
        existing.setName(category.getName());
        existing.setSlug(category.getSlug() == null || category.getSlug().isBlank() ? buildSlug(category.getName()) : buildSlug(category.getSlug()));
        existing.setImageUrl(assetStorageService.normalizeAssetUrl(category.getImageUrl(), "category"));
        existing.setActive(category.isActive());
        if (previousImageUrl != null && !previousImageUrl.equals(existing.getImageUrl())) {
            assetStorageService.deleteManagedAsset(previousImageUrl);
        }
        return categoryRepository.save(existing);
    }

    @Override
    public ProductImageUploadResponse uploadCategoryImage(MultipartFile file) {
        return new ProductImageUploadResponse(assetStorageService.storeImage(file, "category", "category-image.jpg"));
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            throw new ResourceNotFoundException("Category not found");
        }
        categoryRepository.deleteById(id);
        assetStorageService.deleteManagedAsset(category.getImageUrl());
    }

    private Category normalizeCategoryImage(Category category) {
        String normalizedImageUrl = assetStorageService.normalizeAssetUrl(category.getImageUrl(), "category");
        if ((category.getImageUrl() == null && normalizedImageUrl != null) ||
                (category.getImageUrl() != null && !category.getImageUrl().equals(normalizedImageUrl))) {
            category.setImageUrl(normalizedImageUrl);
            return categoryRepository.save(category);
        }

        return category;
    }

    private void validateBulkCategoryRow(Category category) {
        if (category == null) {
            throw new BadRequestException("Category row is empty.");
        }

        if (category.getName() == null || category.getName().isBlank()) {
            throw new BadRequestException("Category name is required.");
        }
    }

    private String buildSlug(String value) {
        return value == null
                ? ""
                : value.trim()
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("(^-|-$)", "");
    }
}
