package com.azimali.loanplatform.modules.risk;

import com.azimali.loanplatform.common.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/risks")
@SecurityRequirement(name = "bearerAuth")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<ApiResponse<RiskScoreDTO>> getRiskScore(@PathVariable UUID loanId) {
        RiskScore riskScore = riskService.getByLoanId(loanId);
        return ResponseEntity.ok(ApiResponse.success(new RiskScoreDTO(riskScore)));
    }

    // Simple DTO to avoid lazy loading issues
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