package com.assetmanagement.asset_management.dto;

import com.assetmanagement.asset_management.enums.ApprovalRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalTaskResponse {

    private Long id;

    private Long approvalRequestId;

    private Long roleId;

    private String roleName;

    private Integer stepOrder;

    private ApprovalRequestStatus status;

    private LocalDateTime approvedAt;

    private Long approvedById;

    private String approvedByName;

    private String note;

    private String assetCode;

    private String assetName;

    private String requesterName;

    private LocalDateTime requestCreatedAt;

    // Người đang gọi API có được phép duyệt/từ chối task này ngay lúc này hay không
    // (đúng vai trò, đúng phạm vi phòng ban/chi nhánh, không phải người yêu cầu,
    // đúng bước hiện tại). Chỉ được điền ở các API trả task theo người dùng hiện tại;
    // null ở nơi khác.
    private Boolean canDecide;
}