package com.assetmanagement.asset_management.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssetCategoryRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String description;
}