package com.akgeneralstore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductBulkImportResponse {
    private int totalRows;
    private int createdCount;
    private int updatedCount;
    private int failedCount;
    private List<ProductResponse> products;
    private List<String> errors;
}
