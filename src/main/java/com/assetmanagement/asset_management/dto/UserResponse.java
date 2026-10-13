package com.assetmanagement.asset_management.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String name;
    private String email;
    private Long departmentId;
    private Long branchId;
    private String departmentName;
    private String branchName;
    private List<String> roles;
}