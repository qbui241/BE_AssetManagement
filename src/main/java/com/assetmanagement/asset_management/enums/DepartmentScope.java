package com.assetmanagement.asset_management.enums;

public enum DepartmentScope {
    REQUESTER_DEPARTMENT,  // Lấy department của người tạo yêu cầu
    ASSET_DEPARTMENT,      // Lấy department đang quản lý asset
    SPECIFIC_DEPARTMENT, // Lấy department_id cố định cấu hình ở step
    REQUESTER_BRANCH,
    ASSET_BRANCH,
    SPECIFIC_BRANCH,
    ANY                    // Không ràng buộc department (ví dụ: DIRECTOR)
}