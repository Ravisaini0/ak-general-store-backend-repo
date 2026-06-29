package com.akgeneralstore.controller;

import com.akgeneralstore.dto.request.ProductRequest;
import com.akgeneralstore.dto.response.ApiResponse;
import com.akgeneralstore.dto.response.ProductBulkImportResponse;
import com.akgeneralstore.dto.response.ProductImageUploadResponse;
import com.akgeneralstore.dto.response.ProductResponse;
import com.akgeneralstore.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/api/products")
    public ApiResponse<List<ProductResponse>> getProducts(@RequestParam(required = false) String search) {
        return new ApiResponse<>(true, "Products fetched", productService.getAllProducts(search));
    }

    @GetMapping("/api/products/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Long id) {
        return new ApiResponse<>(true, "Product fetched", productService.getProductById(id));
    }

    @PostMapping("/api/admin/products")
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return new ApiResponse<>(true, "Product created", productService.createProduct(request));
    }

    @PostMapping("/api/admin/products/bulk-import")
    public ApiResponse<ProductBulkImportResponse> bulkImportProducts(@RequestBody List<ProductRequest> requests) {
        return new ApiResponse<>(true, "Products imported", productService.bulkImportProducts(requests));
    }

    @PutMapping("/api/admin/products/{id}")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return new ApiResponse<>(true, "Product updated", productService.updateProduct(id, request));
    }

    @PostMapping("/api/admin/products/upload-image")
    public ApiResponse<ProductImageUploadResponse> uploadProductImage(@RequestParam("file") MultipartFile file) {
        return new ApiResponse<>(true, "Product image uploaded", productService.uploadProductImage(file));
    }

    @DeleteMapping("/api/admin/products/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return new ApiResponse<>(true, "Product deleted", null);
    }
}
