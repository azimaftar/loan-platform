package com.azimali.loanplatform.modules.decisions;

import com.azimali.loanplatform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/decisions")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Decisions", description = "Automated decisions made by the risk engine")
public class DecisionController {

    private final DecisionService decisionService;

    public DecisionController(DecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @GetMapping("/{loanId}")
    @Operation(summary = "Get decision", description = "Returns the automated decision and reason for a loan")
    public ResponseEntity<ApiResponse<DecisionDTO>> getDecision(@PathVariable UUID loanId) {
        Decision decision = decisionService.getByLoanId(loanId);
        return ResponseEntity.ok(ApiResponse.success(new DecisionDTO(decision)));
    }

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