package com.assetmanagement.asset_management.dto;

import com.assetmanagement.asset_management.enums.AssetStatus;
import com.assetmanagement.asset_management.enums.AssetTrackingType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetResponse {

    private Long id;

    private String assetCode;

    private String name;

    private String serialNumber;

    private AssetTrackingType trackingType;

    private Integer quantity;

    private Integer availableQuantity;

    private BigDecimal value;

    private LocalDate purchaseDate;

    private AssetStatus status;

    private Long categoryId;

    private String categoryName;

    private Long departmentId;

    private String departmentName;

    private Long assignedToId;

    private String assignedToName;

    private List<AssetAttributeValueResponse> attributes;
}