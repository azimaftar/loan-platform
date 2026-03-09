package com.azimali.loanplatform.modules.audit;

import com.azimali.loanplatform.common.ApiResponse;
import com.azimali.loanplatform.modules.users.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Audit", description = "Audit trail for all system actions")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get my audit logs", description = "Returns all actions performed by the current user")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getMyLogs(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLog> logs = auditService.getByUserId(currentUser.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user audit logs", description = "Admin only — returns all actions for any user")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getUserLogs(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLog> logs = auditService.getByUserId(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/loan/{loanId}")
    @Operation(summary = "Get loan audit trail", description = "Returns full history of all actions on a specific loan")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getLoanLogs(
            @PathVariable String loanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLog> logs = auditService.getByResource("LOAN", loanId, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}