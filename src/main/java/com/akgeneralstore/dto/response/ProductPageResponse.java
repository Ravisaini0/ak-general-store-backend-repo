package com.akgeneralstore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductPageResponse {
    private List<ProductResponse> products;
    private int page;
    private int size;
    private int totalPages;
    private int totalItems;
}
