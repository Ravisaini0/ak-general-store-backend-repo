package com.akgeneralstore.service;

import com.akgeneralstore.dto.request.ProductRequest;
import com.akgeneralstore.dto.response.ProductBulkImportResponse;
import com.akgeneralstore.dto.response.ProductImageUploadResponse;
import com.akgeneralstore.dto.response.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    List<ProductResponse> getAllProducts(String search);
    ProductResponse getProductById(Long id);
    ProductResponse createProduct(ProductRequest request);
    ProductBulkImportResponse bulkImportProducts(List<ProductRequest> requests);
    ProductResponse updateProduct(Long id, ProductRequest request);
    void deleteProduct(Long id);
    ProductImageUploadResponse uploadProductImage(MultipartFile file);
}
