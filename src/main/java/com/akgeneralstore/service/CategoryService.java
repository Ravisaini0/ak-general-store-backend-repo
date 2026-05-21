package com.akgeneralstore.service;

import com.akgeneralstore.dto.response.ProductImageUploadResponse;
import com.akgeneralstore.entity.Category;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CategoryService {
    List<Category> getAllCategories();
    Category createCategory(Category category);
    Category updateCategory(Long id, Category category);
    ProductImageUploadResponse uploadCategoryImage(MultipartFile file);
    void deleteCategory(Long id);
}
