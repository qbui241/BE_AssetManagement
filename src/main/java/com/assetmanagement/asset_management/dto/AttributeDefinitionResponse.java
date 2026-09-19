package com.assetmanagement.asset_management.dto;

import com.assetmanagement.asset_management.enums.AttributeDataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributeDefinitionResponse {

    private Long id;

    private Long categoryId;

    private String categoryName;

    private String name;

    private String label;

    private AttributeDataType dataType;

    private boolean required;
}
