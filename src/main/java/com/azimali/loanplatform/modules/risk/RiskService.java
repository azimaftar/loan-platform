package com.azimali.loanplatform.modules.risk;

import com.azimali.loanplatform.common.exception.ResourceNotFoundException;
import com.azimali.loanplatform.modules.loans.Loan;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RiskService {

    private final RiskScoreRepository riskScoreRepository;
    private final RiskScoringEngine riskScoringEngine;

    public RiskService(RiskScoreRepository riskScoreRepository,
                       RiskScoringEngine riskScoringEngine) {
        this.riskScoreRepository = riskScoreRepository;
        this.riskScoringEngine = riskScoringEngine;
    }

    // Called automatically when a loan is submitted
    public RiskScore computeAndSave(Loan loan) {
        RiskScoringEngine.ScoringResult result = riskScoringEngine.score(loan);

        RiskScore riskScore = new RiskScore();
        riskScore.setLoan(loan);
        riskScore.setProbability(result.probability());
        riskScore.setFactors(result.factors());

        return riskScoreRepository.save(riskScore);
    }

    // Get risk score for a loan — used by the REST endpoint
    public RiskScore getByLoanId(UUID loanId) {
        return riskScoreRepository.findByLoanId(loanId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Risk score not found for loan: " + loanId));
    }
}