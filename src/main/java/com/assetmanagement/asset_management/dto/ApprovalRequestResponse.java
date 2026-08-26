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
public class ApprovalRequestResponse {

    private Long id;

    private Long assetId;
    private String assetCode;
    private String assetName;

    private Long requesterId;
    private String requesterName;

    private Long workflowId;
    private String workflowName;

    private Integer currentStepOrder;

    private ApprovalRequestStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}