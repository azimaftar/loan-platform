package com.azimali.loanplatform.modules.audit;

import com.azimali.loanplatform.modules.users.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // Main method — called by other services to log actions
    public void log(User user, String action, String resourceType, String resourceId, String details) {
        AuditLog log = new AuditLog();
        if (user != null) {
            log.setUserId(user.getId());
            log.setUsername(user.getUsername());
        }
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    // Overload for system actions with no user
    public void log(String action, String resourceType, String resourceId, String details) {
        log(null, action, resourceType, resourceId, details);
    }

    // Get logs by user
    public Page<AuditLog> getByUserId(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    // Get logs by resource
    public Page<AuditLog> getByResource(String resourceType, String resourceId, Pageable pageable) {
        return auditLogRepository.findByResourceTypeAndResourceId(
                resourceType, resourceId, pageable);
    }
}