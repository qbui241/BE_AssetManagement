package com.assetmanagement.asset_management.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class AssetRequest {

    @NotBlank(message = "assetCode is required")
    private String assetCode;

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "serialNumber is required")
    private String serialNumber;

    @NotNull(message = "Asset value is required")
    @Positive(message = "Asset value must be greater than 0")
    private BigDecimal value;

    @NotNull(message = "purchaseDate is required")
    private LocalDate purchaseDate;

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    @NotNull(message = "departmentId is required")
    private Long departmentId;

    @Valid
    private List<AssetAttributeValueRequest> attributes = new ArrayList<>();
}