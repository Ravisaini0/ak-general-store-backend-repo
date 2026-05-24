package com.akgeneralstore.service.impl;

import com.akgeneralstore.dto.response.ProductImageUploadResponse;
import com.akgeneralstore.entity.Category;
import com.akgeneralstore.exception.ResourceNotFoundException;
import com.akgeneralstore.repository.CategoryRepository;
import com.akgeneralstore.service.AssetStorageService;
import com.akgeneralstore.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
            category.setSlug(category.getName().trim().toLowerCase().replace(" ", "-"));
        }
        category.setImageUrl(assetStorageService.normalizeAssetUrl(category.getImageUrl(), "category"));
        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(Long id, Category category) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        String previousImageUrl = existing.getImageUrl();
        existing.setName(category.getName());
        existing.setSlug(category.getSlug());
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
}
