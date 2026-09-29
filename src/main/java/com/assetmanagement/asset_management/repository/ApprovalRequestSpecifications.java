package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.ApprovalRequest;
import com.assetmanagement.asset_management.enums.ApprovalRequestStatus;
import org.springframework.data.jpa.domain.Specification;

public class ApprovalRequestSpecifications {

    private ApprovalRequestSpecifications() {
    }

    public static Specification<ApprovalRequest> withFilters(
            ApprovalRequestStatus status,
            Long requesterId,
            Long assetBranchId) {

        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (requesterId != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("requester").get("id"), requesterId));
            }
            if (assetBranchId != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("asset").get("department").get("branch").get("id"), assetBranchId));
            }

            return predicate;
        };
    }
}
