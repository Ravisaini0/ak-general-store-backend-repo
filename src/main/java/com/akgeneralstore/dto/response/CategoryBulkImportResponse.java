package com.akgeneralstore.dto.response;

import com.akgeneralstore.entity.Category;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CategoryBulkImportResponse {
    private int totalRows;
    private int createdCount;
    private int updatedCount;
    private int failedCount;
    private List<Category> categories;
    private List<String> errors;
}
