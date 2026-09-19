package com.assetmanagement.asset_management.dto;

import com.assetmanagement.asset_management.enums.AttributeDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttributeDefinitionRequest {

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "label is required")
    private String label;

    @NotNull(message = "dataType is required")
    private AttributeDataType dataType;

    private boolean required;
}
