package com.azimali.loanplatform.modules.audit;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Who performed the action — null for system actions
    @Column
    private UUID userId;

    @Column
    private String username;

    // What action was performed
    @Column(nullable = false)
    private String action;

    // What type of resource was affected
    @Column
    private String resourceType;

    // The ID of the affected resource
    @Column
    private String resourceId;

    // Extra details about the action
    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}