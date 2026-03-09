package com.azimali.loanplatform.modules.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    // Get all logs for a specific user
    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);

    // Get all logs for a specific resource
    Page<AuditLog> findByResourceTypeAndResourceId(
            String resourceType, String resourceId, Pageable pageable);
}