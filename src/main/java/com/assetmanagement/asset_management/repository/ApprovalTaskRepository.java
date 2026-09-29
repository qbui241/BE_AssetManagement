package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.ApprovalTask;
import com.assetmanagement.asset_management.enums.ApprovalRequestStatus;
import com.assetmanagement.asset_management.enums.WorkflowType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ApprovalTaskRepository
        extends JpaRepository<ApprovalTask, Long> {

    List<ApprovalTask> findByApprovalRequestIdOrderByStepOrderAsc(
            Long approvalRequestId
    );

    List<ApprovalTask> findByRoleIdAndStatusOrderByStepOrderAsc(
            Long roleId,
            ApprovalRequestStatus status
    );

    List<ApprovalTask> findByRoleIdOrderByStepOrderAsc(
            Long roleId
    );

    List<ApprovalTask> findByStatusOrderByStepOrderAsc(
            ApprovalRequestStatus status
    );

    // ===== Danh sách phân trang (chỉ ADMIN dùng, xem toàn hệ thống) =====
    Page<ApprovalTask> findByRoleIdAndStatus(
            Long roleId,
            ApprovalRequestStatus status,
            Pageable pageable
    );

    Page<ApprovalTask> findByRoleId(Long roleId, Pageable pageable);

    Page<ApprovalTask> findByStatus(ApprovalRequestStatus status, Pageable pageable);

    @Query(value = """
            SELECT t FROM ApprovalTask t
            JOIN t.approvalRequest r
            WHERE t.status = :pending
              AND r.status = :pending
              AND r.requester.id <> :userId
              AND t.role.id IN :roleIds
              AND (t.department IS NULL OR t.department.id = :departmentId)
              AND (t.branch IS NULL OR t.branch.id = :branchId)
              AND (r.workflow.type <> :sequential OR t.stepOrder = r.currentStepOrder)
            """,
            countQuery = """
            SELECT COUNT(t) FROM ApprovalTask t
            JOIN t.approvalRequest r
            WHERE t.status = :pending
              AND r.status = :pending
              AND r.requester.id <> :userId
              AND t.role.id IN :roleIds
              AND (t.department IS NULL OR t.department.id = :departmentId)
              AND (t.branch IS NULL OR t.branch.id = :branchId)
              AND (r.workflow.type <> :sequential OR t.stepOrder = r.currentStepOrder)
            """)
    Page<ApprovalTask> findPendingForApprover(
            @Param("pending") ApprovalRequestStatus pending,
            @Param("sequential") WorkflowType sequential,
            @Param("userId") Long userId,
            @Param("roleIds") Collection<Long> roleIds,
            @Param("departmentId") Long departmentId,
            @Param("branchId") Long branchId,
            Pageable pageable
    );

    List<ApprovalTask> findByApprovalRequestId(
            Long approvalRequestId
    );

    @Modifying
    @Query("UPDATE ApprovalTask t SET t.status = 'REJECTED' " +
            "WHERE t.approvalRequest.id = :requestId AND t.status = 'PENDING'")
    void rejectAllPendingTasksByRequestId(@Param("requestId") Long requestId);

    @Modifying
    @Query("UPDATE ApprovalTask t SET t.status = 'CANCELLED', t.note = :note " +
            "WHERE t.approvalRequest.id = :requestId AND t.stepOrder = :stepOrder AND t.status = 'PENDING'")
    void cancelPendingSiblingTasks(
            @Param("requestId") Long requestId,
            @Param("stepOrder") Integer stepOrder,
            @Param("note") String note
    );

    boolean existsByApprovalRequestIdAndStepOrderAndStatus(
            Long approvalRequestId,
            Integer stepOrder,
            ApprovalRequestStatus status
    );
}