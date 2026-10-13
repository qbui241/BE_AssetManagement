package com.assetmanagement.asset_management.dto;

import lombok.Data;

/** ADMIN dat lai mat khau cho nguoi khac - khong can mat khau cu. */
@Data
public class ResetPasswordRequest {
    private String newPassword;
}