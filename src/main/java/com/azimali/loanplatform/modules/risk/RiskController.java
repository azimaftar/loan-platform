package com.azimali.loanplatform.modules.risk;

import com.azimali.loanplatform.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/risks")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Risk", description = "Risk scores for loan applications")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @GetMapping("/{loanId}")
    @Operation(summary = "Get risk score", description = "Returns the risk probability score and contributing factors for a loan")
    public ResponseEntity<ApiResponse<RiskScoreDTO>> getRiskScore(@PathVariable UUID loanId) {
        RiskScore riskScore = riskService.getByLoanId(loanId);
        return ResponseEntity.ok(ApiResponse.success(new RiskScoreDTO(riskScore)));
    }

    public record RiskScoreDTO(
            UUID id,
            UUID loanId,
            BigDecimal probability,
            String factors,
            LocalDateTime createdAt
    ) {
        public RiskScoreDTO(RiskScore r) {
            this(r.getId(), r.getLoan().getId(), r.getProbability(), r.getFactors(), r.getCreatedAt());
        }
    }
}