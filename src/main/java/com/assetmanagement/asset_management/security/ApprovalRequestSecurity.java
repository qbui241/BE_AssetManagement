package com.assetmanagement.asset_management.security;

import com.assetmanagement.asset_management.repository.ApprovalRequestRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class ApprovalRequestSecurity {
    private final ApprovalRequestRepository approvalRequestRepository;

    public ApprovalRequestSecurity(ApprovalRequestRepository approvalRequestRepository) {
        this.approvalRequestRepository = approvalRequestRepository;
    }

    public boolean isRequester(
            Long requestId,
            Authentication authentication
    ){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();
        return approvalRequestRepository.existsByIdAndRequesterId(requestId, userId);
    }
}
