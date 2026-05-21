package com.akgeneralstore.dto.request;

import com.akgeneralstore.enums.CollectionMethod;
import lombok.Data;

@Data
public class DeliveryCompletionRequest {
    private CollectionMethod collectionMethod;
    private String referenceId;
}
