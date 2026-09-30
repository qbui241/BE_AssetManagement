package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.Asset;
import com.assetmanagement.asset_management.enums.AssetStatus;
import com.assetmanagement.asset_management.enums.AssetTrackingType;
import org.springframework.data.jpa.domain.Specification;

public class AssetSpecifications {

    private AssetSpecifications() {
    }

    /**
     * @param status        lọc theo trạng thái
     * @param categoryId    lọc theo danh mục
     * @param trackingType  lọc theo cách quản lý (INDIVIDUAL/BULK)
     * @param branchId      chỉ lấy tài sản thuộc chi nhánh này (dùng cho ABAC, null = không giới hạn)
     * @param keyword       tìm không phân biệt hoa thường trong mã tài sản, tên, số serial
     */
    public static Specification<Asset> withFilters(
            AssetStatus status,
            Long categoryId,
            AssetTrackingType trackingType,
            Long branchId,
            String keyword) {

        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("category").get("id"), categoryId));
            }
            if (trackingType != null) {
                predicate = cb.and(predicate, cb.equal(root.get("trackingType"), trackingType));
            }
            if (branchId != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("department").get("branch").get("id"), branchId));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("assetCode")), like),
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.<String>get("serialNumber")), like)
                ));
            }

            return predicate;
        };
    }
}
