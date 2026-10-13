package com.assetmanagement.asset_management.dto;

import lombok.Data;

/** Nguoi dung tu doi mat khau cua chinh minh - can nhap dung mat khau hien tai. */
@Data
public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;
}