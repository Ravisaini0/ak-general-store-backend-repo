package com.akgeneralstore.service.impl;

import com.akgeneralstore.config.ProductUploadProperties;
import com.akgeneralstore.dto.request.ProductRequest;
import com.akgeneralstore.dto.response.ProductImageUploadResponse;
import com.akgeneralstore.dto.response.ProductResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.akgeneralstore.entity.Product;
import com.akgeneralstore.exception.BadRequestException;
import com.akgeneralstore.exception.ResourceNotFoundException;
import com.akgeneralstore.repository.ProductRepository;
import com.akgeneralstore.service.ProductService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {
    private static final Path PRODUCT_UPLOAD_DIRECTORY = Paths.get("uploads", "products");
    private static final Map<String, List<String>> SEARCH_ALIASES = createSearchAliases();

    private final ProductRepository productRepository;
    private final ProductUploadProperties productUploadProperties;
    private final ObjectMapper objectMapper;

    public ProductServiceImpl(
            ProductRepository productRepository,
            ProductUploadProperties productUploadProperties,
            ObjectMapper objectMapper
    ) {
        this.productRepository = productRepository;
        this.productUploadProperties = productUploadProperties;
        this.objectMapper = objectMapper;
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
            Files.createDirectories(PRODUCT_UPLOAD_DIRECTORY);
            Path targetPath = PRODUCT_UPLOAD_DIRECTORY.resolve(fileName).normalize();

            if (!targetPath.startsWith(PRODUCT_UPLOAD_DIRECTORY.normalize())) {
                throw new BadRequestException("Invalid upload target path.");
            }

            Files.copy(new ByteArrayInputStream(fileBytes), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return new ProductImageUploadResponse("/uploads/products/" + fileName);
        } catch (BadRequestException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BadRequestException("Product image could not be uploaded.");
        }
    }

    private Product toProduct(Product product, ProductRequest request) {
        List<String> normalizedImageUrls = normalizeImageUrls(request.getImageUrls(), request.getImageUrl());
        product.setName(request.getName());
        product.setSlug(request.getName().trim().toLowerCase().replace(" ", "-"));
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setUnit(request.getUnit());
        product.setCategoryId(request.getCategoryId());
        product.setFeatured(request.isFeatured());
        product.setImageUrl(normalizedImageUrls.isEmpty() ? request.getImageUrl() : normalizedImageUrls.get(0));
        product.setImageGallery(writeImageGallery(normalizedImageUrls));
        return product;
    }

    private ProductResponse mapProduct(Product product) {
        List<String> imageUrls = readImageGallery(product.getImageGallery(), product.getImageUrl());
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .unit(product.getUnit())
                .imageUrl(product.getImageUrl())
                .imageUrls(imageUrls)
                .featured(product.isFeatured())
                .categoryId(product.getCategoryId())
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

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitizeOriginalName(String originalName) {
        String fallbackName = originalName == null || originalName.isBlank() ? "product-image.jpg" : originalName;
        String normalized = Normalizer.normalize(fallbackName, Normalizer.Form.NFKC)
                .replace("\\", "")
                .replace("/", "")
                .trim();

        if (normalized.isBlank()) {
            return "product-image.jpg";
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
