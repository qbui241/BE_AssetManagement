package com.assetmanagement.asset_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssetAttributeValueRequest {

    @NotNull(message = "attributeDefinitionId is required")
    private Long attributeDefinitionId;

    // Luôn nhận dạng String từ client; ép kiểu/validate theo
    // attributeDefinition.dataType ở AssetService.
    private String value;
}
