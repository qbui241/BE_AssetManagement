package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> withFilters(
            String action,
            String entityType,
            LocalDateTime from,
            LocalDateTime to) {

        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (action != null && !action.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("action"), action));
            }
            if (entityType != null && !entityType.isBlank()) {
                predicate = cb.and(predicate, cb.equal(root.get("entityType"), entityType));
            }
            if (from != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return predicate;
        };
    }
}
