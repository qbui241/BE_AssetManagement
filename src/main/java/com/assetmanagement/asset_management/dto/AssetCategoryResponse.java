package com.assetmanagement.asset_management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetCategoryResponse {

    private Long id;
    private String name;
    private String description;
}