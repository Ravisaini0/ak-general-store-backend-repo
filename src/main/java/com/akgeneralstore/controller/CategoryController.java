package com.akgeneralstore.controller;

import com.akgeneralstore.dto.response.ApiResponse;
import com.akgeneralstore.dto.response.ProductImageUploadResponse;
import com.akgeneralstore.entity.Category;
import com.akgeneralstore.service.CategoryService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/api/categories")
    public ApiResponse<List<Category>> getCategories() {
        return new ApiResponse<>(true, "Categories fetched", categoryService.getAllCategories());
    }

    @PostMapping("/api/admin/categories")
    public ApiResponse<Category> createCategory(@RequestBody Category category) {
        return new ApiResponse<>(true, "Category created", categoryService.createCategory(category));
    }

    @PutMapping("/api/admin/categories/{id}")
    public ApiResponse<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        return new ApiResponse<>(true, "Category updated", categoryService.updateCategory(id, category));
    }

    @PostMapping("/api/admin/categories/upload-image")
    public ApiResponse<ProductImageUploadResponse> uploadCategoryImage(@RequestParam("file") MultipartFile file) {
        return new ApiResponse<>(true, "Category image uploaded", categoryService.uploadCategoryImage(file));
    }

    @DeleteMapping("/api/admin/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return new ApiResponse<>(true, "Category deleted", null);
    }
}
