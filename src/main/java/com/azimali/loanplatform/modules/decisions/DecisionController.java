package com.azimali.loanplatform.modules.decisions;

import com.azimali.loanplatform.common.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/decisions")
@SecurityRequirement(name = "bearerAuth")
public class DecisionController {

    private final DecisionService decisionService;

    public DecisionController(DecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<ApiResponse<DecisionDTO>> getDecision(@PathVariable UUID loanId) {
        Decision decision = decisionService.getByLoanId(loanId);
        return ResponseEntity.ok(ApiResponse.success(new DecisionDTO(decision)));
    }

    // Simple DTO to avoid lazy loading issues
    public record DecisionDTO(
            UUID id,
            UUID loanId,
            String decision,
            String reason,
            LocalDateTime createdAt
    ) {
        public DecisionDTO(Decision d) {
            this(d.getId(), d.getLoan().getId(), d.getDecision().name(), d.getReason(), d.getCreatedAt());
        }
    }
}