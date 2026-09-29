package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.AssetHistory;
import org.springframework.data.jpa.domain.Specification;

public class AssetHistorySpecifications {

    private AssetHistorySpecifications() {
    }

    /**
     * @param assetId       chỉ lấy lịch sử của 1 tài sản
     * @param userId        chỉ lấy lịch sử của 1 người mượn
     * @param openOnly      true = chỉ lấy các lượt mượn chưa trả (returnedAt null)
     * @param keyword       tìm không phân biệt hoa thường trong mã/tên tài sản,
     *                      tên/email người mượn
     * @param assetBranchId chỉ lấy lượt mượn của tài sản thuộc chi nhánh này (ABAC)
     */
    public static Specification<AssetHistory> withFilters(
            Long assetId,
            Long userId,
            boolean openOnly,
            String keyword,
            Long assetBranchId) {

        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (assetId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("asset").get("id"), assetId));
            }
            if (userId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("user").get("id"), userId));
            }
            if (openOnly) {
                predicate = cb.and(predicate, cb.isNull(root.get("returnedAt")));
            }
            if (assetBranchId != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("asset").get("department").get("branch").get("id"), assetBranchId));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("asset").<String>get("assetCode")), like),
                        cb.like(cb.lower(root.get("asset").<String>get("name")), like),
                        cb.like(cb.lower(root.get("user").<String>get("name")), like),
                        cb.like(cb.lower(root.get("user").<String>get("email")), like)
                ));
            }

            return predicate;
        };
    }
}
