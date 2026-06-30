package com.akgeneralstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private BigDecimal price;

    private BigDecimal originalPrice;
    private String unit;
    private Long categoryId;
    private List<Long> categoryIds;
    private boolean featured;
    private String imageUrl;
    private List<String> imageUrls;
}
