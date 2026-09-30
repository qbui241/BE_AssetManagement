package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.User;
import org.springframework.data.jpa.domain.Specification;

/**
 * Điều kiện lọc động cho danh sách người dùng. Mọi tham số đều optional.
 */
public class UserSpecifications {

    private UserSpecifications() {
    }

    /**
     * @param departmentId lọc theo phòng ban
     * @param roleName     lọc theo tên vai trò (MANAGER/DIRECTOR/ADMIN)
     * @param keyword      tìm không phân biệt hoa thường trong họ tên, tài khoản, email
     * @param branchId     chỉ lấy user thuộc chi nhánh này (ABAC, null = không giới hạn)
     */
    public static Specification<User> withFilters(
            Long departmentId,
            String roleName,
            String keyword,
            Long branchId) {

        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (departmentId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("department").get("id"), departmentId));
            }
            if (roleName != null && !roleName.isBlank()) {
                var roles = root.join("roles");
                predicate = cb.and(predicate, cb.equal(roles.get("name"), roleName));
            }
            if (branchId != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("department").get("branch").get("id"), branchId));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("username")), like),
                        cb.like(cb.lower(root.get("email")), like)
                ));
            }

            return predicate;
        };
    }
}
