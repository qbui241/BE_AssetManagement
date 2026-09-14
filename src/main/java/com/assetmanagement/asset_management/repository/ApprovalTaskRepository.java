package com.assetmanagement.asset_management.repository;

import com.assetmanagement.asset_management.entity.ApprovalTask;
import com.assetmanagement.asset_management.enums.ApprovalRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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