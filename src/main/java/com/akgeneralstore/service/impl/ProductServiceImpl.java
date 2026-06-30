package com.akgeneralstore.service.impl;

import com.akgeneralstore.dto.request.ProductRequest;
import com.akgeneralstore.dto.response.ProductBulkImportResponse;
import com.akgeneralstore.dto.response.ProductImageUploadResponse;
import com.akgeneralstore.dto.response.ProductResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.akgeneralstore.entity.Product;
import com.akgeneralstore.exception.BadRequestException;
import com.akgeneralstore.exception.ResourceNotFoundException;
import com.akgeneralstore.repository.CategoryRepository;
import com.akgeneralstore.repository.ProductRepository;
import com.akgeneralstore.service.AssetStorageService;
import com.akgeneralstore.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {
    private static final Map<String, List<String>> SEARCH_ALIASES = createSearchAliases();

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;
    private final AssetStorageService assetStorageService;

    public ProductServiceImpl(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ObjectMapper objectMapper,
            AssetStorageService assetStorageService
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.objectMapper = objectMapper;
        this.assetStorageService = assetStorageService;
    }

    @Override
    public List<ProductResponse> getAllProducts(String search) {
        List<Product> products = productRepository.findAll();

        if (search == null || search.isBlank()) {
            return products.stream().map(this::mapProduct).toList();
        }

        String normalizedSearch = search.trim().toLowerCase(Locale.ROOT);
        List<String> tokens = expandSearchTokens(normalizedSearch);

        return products.stream()
                .map(product -> Map.entry(product, scoreProduct(product, normalizedSearch, tokens)))
                .filter(entry -> entry.getValue() > 0)
                .sorted((left, right) -> {
                    int scoreCompare = Integer.compare(right.getValue(), left.getValue());
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }

                    return left.getKey().getName().compareToIgnoreCase(right.getKey().getName());
                })
                .map(Map.Entry::getKey)
                .map(this::mapProduct)
                .toList();
    }

    @Override
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return mapProduct(product);
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        Product product = toProduct(new Product(), request);
        return mapProduct(productRepository.save(product));
    }

    @Override
    public ProductBulkImportResponse bulkImportProducts(List<ProductRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("Please upload at least one product row.");
        }

        List<ProductResponse> importedProducts = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int createdCount = 0;
        int updatedCount = 0;

        for (int index = 0; index < requests.size(); index++) {
            ProductRequest request = requests.get(index);
            int rowNumber = index + 1;

            try {
                validateBulkProductRow(request, rowNumber);
                String slug = buildSlug(request.getName());
                Product product = productRepository.findBySlug(slug).orElseGet(Product::new);
                boolean isNewProduct = product.getId() == null;
                Product savedProduct = productRepository.save(toProduct(product, request));
                importedProducts.add(mapProduct(savedProduct));

                if (isNewProduct) {
                    createdCount++;
                } else {
                    updatedCount++;
                }
            } catch (Exception exception) {
                errors.add("Row " + rowNumber + ": " + exception.getMessage());
            }
        }

        return ProductBulkImportResponse.builder()
                .totalRows(requests.size())
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .failedCount(errors.size())
                .products(importedProducts)
                .errors(errors)
                .build();
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return mapProduct(productRepository.save(toProduct(product, request)));
    }

    @Override
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found");
        }
        productRepository.deleteById(id);
    }

    @Override
    public ProductImageUploadResponse uploadProductImage(MultipartFile file) {
        return new ProductImageUploadResponse(assetStorageService.storeImage(file, "product", "product-image.jpg"));
    }

    private Product toProduct(Product product, ProductRequest request) {
        List<String> previousImageUrls = readImageGallery(product.getImageGallery(), product.getImageUrl());
        List<String> normalizedImageUrls = normalizeImageUrls(request.getImageUrls(), request.getImageUrl()).stream()
                .map(url -> assetStorageService.normalizeAssetUrl(url, "product"))
                .collect(Collectors.toList());
        product.setName(request.getName());
        product.setSlug(buildSlug(request.getName()));
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setUnit(request.getUnit());
        List<Long> categoryIds = normalizeCategoryIds(request.getCategoryIds(), request.getCategoryId());
        List<com.akgeneralstore.entity.Category> categories = categoryRepository.findAllById(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new BadRequestException("One or more selected categories were not found.");
        }
        product.setCategoryId(categoryIds.isEmpty() ? null : categoryIds.get(0));
        product.getCategories().clear();
        product.getCategories().addAll(categories);
        product.setFeatured(request.isFeatured());
        product.setImageUrl(normalizedImageUrls.isEmpty() ? request.getImageUrl() : normalizedImageUrls.get(0));
        product.setImageGallery(writeImageGallery(normalizedImageUrls));
        previousImageUrls.stream()
                .filter(previousUrl -> normalizedImageUrls.stream().noneMatch(currentUrl -> Objects.equals(currentUrl, previousUrl)))
                .forEach(assetStorageService::deleteManagedAsset);
        return product;
    }

    private ProductResponse mapProduct(Product product) {
        List<String> rawImageUrls = readImageGallery(product.getImageGallery(), product.getImageUrl());
        List<String> imageUrls = rawImageUrls.stream()
                .map(url -> assetStorageService.normalizeAssetUrl(url, "product"))
                .distinct()
                .collect(Collectors.toList());
        String primaryImageUrl = assetStorageService.normalizeAssetUrl(product.getImageUrl(), "product");
        String resolvedPrimaryImage = !imageUrls.isEmpty()
                ? imageUrls.get(0)
                : (primaryImageUrl == null ? "" : primaryImageUrl.trim());
        List<Long> categoryIds = getProductCategoryIds(product);

        if (!Objects.equals(product.getImageUrl(), resolvedPrimaryImage) || !rawImageUrls.equals(imageUrls)) {
            product.setImageUrl(resolvedPrimaryImage);
            product.setImageGallery(writeImageGallery(imageUrls));
            productRepository.save(product);
        }

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .unit(product.getUnit())
                .imageUrl(resolvedPrimaryImage)
                .imageUrls(imageUrls)
                .featured(product.isFeatured())
                .categoryId(categoryIds.isEmpty() ? product.getCategoryId() : categoryIds.get(0))
                .categoryIds(categoryIds)
                .build();
    }

    private int scoreProduct(Product product, String normalizedSearch, List<String> tokens) {
        String productName = safeLower(product.getName());
        String productSlug = safeLower(product.getSlug());
        String description = safeLower(product.getDescription());
        String unit = safeLower(product.getUnit());
        String haystack = productName + " " + productSlug + " " + description + " " + unit;

        int score = 0;

        if (productName.equals(normalizedSearch)) {
            score += 150;
        }

        if (productSlug.equals(normalizedSearch.replace(" ", "-"))) {
            score += 130;
        }

        if (productName.contains(normalizedSearch)) {
            score += 90;
        }

        if (productSlug.contains(normalizedSearch)) {
            score += 80;
        }

        for (String token : tokens) {
            if (productName.startsWith(token)) {
                score += 30;
            }

            if (productName.contains(token)) {
                score += 22;
            }

            if (productSlug.contains(token)) {
                score += 18;
            }

            if (description.contains(token)) {
                score += 9;
            }

            if (unit.contains(token)) {
                score += 4;
            }
        }

        return haystack.contains(normalizedSearch) || tokens.stream().anyMatch(haystack::contains)
                ? score
                : 0;
    }

    private List<String> expandSearchTokens(String search) {
        List<String> expanded = new ArrayList<>();

        for (String token : search.replaceAll("[^a-z0-9\\s]", " ").split("\\s+")) {
            if (token == null || token.isBlank()) {
                continue;
            }

            List<String> aliases = SEARCH_ALIASES.getOrDefault(token, List.of(token));
            for (String alias : aliases) {
                if (!expanded.contains(alias)) {
                    expanded.add(alias);
                }
            }
        }

        return expanded;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void validateBulkProductRow(ProductRequest request, int rowNumber) {
        if (request == null) {
            throw new BadRequestException("Product row is empty.");
        }

        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Product name is required.");
        }

        if (request.getPrice() == null) {
            throw new BadRequestException("Price is required.");
        }

        if (normalizeCategoryIds(request.getCategoryIds(), request.getCategoryId()).isEmpty()) {
            throw new BadRequestException("At least one category is required.");
        }
    }

    private List<Long> normalizeCategoryIds(List<Long> categoryIds, Long categoryId) {
        List<Long> normalized = new ArrayList<>();

        if (categoryIds != null) {
            for (Long id : categoryIds) {
                if (id != null && id > 0 && !normalized.contains(id)) {
                    normalized.add(id);
                }
            }
        }

        if (normalized.isEmpty() && categoryId != null && categoryId > 0) {
            normalized.add(categoryId);
        }

        return normalized;
    }

    private List<Long> getProductCategoryIds(Product product) {
        if (product.getCategories() != null && !product.getCategories().isEmpty()) {
            return product.getCategories().stream()
                    .map(com.akgeneralstore.entity.Category::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }

        return product.getCategoryId() == null ? List.of() : List.of(product.getCategoryId());
    }

    private String buildSlug(String name) {
        return name == null
                ? ""
                : name.trim()
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("(^-|-$)", "");
    }

    private List<String> normalizeImageUrls(List<String> imageUrls, String imageUrl) {
        List<String> normalized = (imageUrls == null ? List.<String>of() : imageUrls).stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (normalized.isEmpty() && imageUrl != null && !imageUrl.isBlank()) {
            normalized = List.of(imageUrl.trim());
        }

        return normalized;
    }

    private String writeImageGallery(List<String> imageUrls) {
        try {
            return objectMapper.writeValueAsString(imageUrls == null ? List.of() : imageUrls);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Product image gallery could not be saved.");
        }
    }

    private List<String> readImageGallery(String imageGallery, String imageUrl) {
        if (imageGallery != null && !imageGallery.isBlank()) {
            try {
                List<String> parsed = objectMapper.readValue(imageGallery, new TypeReference<List<String>>() {});
                List<String> cleaned = parsed.stream()
                        .map(value -> value == null ? "" : value.trim())
                        .filter(value -> !value.isBlank())
                        .distinct()
                        .collect(Collectors.toList());
                if (!cleaned.isEmpty()) {
                    return cleaned;
                }
            } catch (JsonProcessingException ignored) {
                // Fall back to the primary image url if legacy data or malformed gallery is present.
            }
        }

        if (imageUrl != null && !imageUrl.isBlank()) {
            return List.of(imageUrl.trim());
        }

        return List.of();
    }

    private static Map<String, List<String>> createSearchAliases() {
        Map<String, List<String>> aliases = new HashMap<>();
        aliases.put("atta", List.of("atta", "aata", "flour", "wheat", "chakki"));
        aliases.put("aata", List.of("aata", "atta", "flour", "wheat", "chakki"));
        aliases.put("flour", List.of("flour", "atta", "aata", "wheat", "chakki"));
        aliases.put("dal", List.of("dal", "dall", "lentil", "lentils", "pulse", "pulses"));
        aliases.put("lentil", List.of("lentil", "lentils", "dal", "pulse", "pulses"));
        aliases.put("lentils", List.of("lentils", "lentil", "dal", "pulse", "pulses"));
        aliases.put("rice", List.of("rice", "basmati", "grain", "grains"));
        aliases.put("oil", List.of("oil", "ghee", "mustard", "sunflower"));
        aliases.put("ghee", List.of("ghee", "oil", "mustard", "sunflower"));
        aliases.put("masala", List.of("masala", "spice", "spices", "seasoning"));
        aliases.put("spice", List.of("spice", "spices", "masala", "seasoning"));
        aliases.put("spices", List.of("spices", "spice", "masala", "seasoning"));
        aliases.put("snack", List.of("snack", "snacks", "namkeen", "biscuits", "biscuit"));
        aliases.put("snacks", List.of("snack", "snacks", "namkeen", "biscuits", "biscuit"));
        aliases.put("biscuit", List.of("biscuit", "biscuits", "snack", "snacks", "namkeen"));
        aliases.put("biscuits", List.of("biscuits", "biscuit", "snack", "snacks", "namkeen"));
        return aliases;
    }
}
