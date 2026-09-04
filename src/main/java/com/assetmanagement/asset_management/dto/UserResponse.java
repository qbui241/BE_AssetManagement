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
    private List<String> roles;
}
