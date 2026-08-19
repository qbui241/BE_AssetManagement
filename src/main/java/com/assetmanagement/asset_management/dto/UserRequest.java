package com.assetmanagement.asset_management.dto;

import lombok.Data;

@Data
public class UserRequest {

    private String name;

    private String email;

    private Long departmentId;
}
