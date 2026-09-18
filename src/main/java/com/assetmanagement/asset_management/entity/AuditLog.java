package com.assetmanagement.asset_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Loai doi tuong bi tac dong, VD: "ASSET", "APPROVAL_REQUEST", "APPROVAL_TASK"
    @Column(nullable = false, length = 50)
    private String entityType;

    // Id cua doi tuong bi tac dong (VD: asset.id, approvalRequest.id)
    @Column(nullable = false)
    private Long entityId;

    // Hanh dong da xay ra, VD: "STATUS_CHANGED", "APPROVED", "REJECTED", "CANCELLED"
    @Column(nullable = false, length = 50)
    private String action;

    // Gia tri truoc khi thay doi (co the null neu la hanh dong tao moi)
    @Column(length = 100)
    private String oldValue;

    // Gia tri sau khi thay doi
    @Column(length = 100)
    private String newValue;

    @Column(length = 500)
    private String description;

    // Nguoi thuc hien hanh dong. NULL neu la he thong tu dong thuc hien.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}