package com.azimali.loanplatform.modules.decisions;

import com.azimali.loanplatform.common.exception.ResourceNotFoundException;
import com.azimali.loanplatform.modules.loans.Loan;
import com.azimali.loanplatform.modules.loans.LoanRepository;
import com.azimali.loanplatform.modules.loans.LoanStatus;
import com.azimali.loanplatform.modules.risk.RiskScore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class DecisionService {

    private final DecisionRepository decisionRepository;
    private final LoanRepository loanRepository;

    @Value("${app.risk.approve-threshold}")
    private BigDecimal approveThreshold;

    @Value("${app.risk.reject-threshold}")
    private BigDecimal rejectThreshold;

    public DecisionService(DecisionRepository decisionRepository,
                           LoanRepository loanRepository) {
        this.decisionRepository = decisionRepository;
        this.loanRepository = loanRepository;
    }

    // Called automatically after risk scoring
    public Decision decide(Loan loan, RiskScore riskScore) {
        BigDecimal probability = riskScore.getProbability();
        DecisionType decisionType;
        String reason;

        if (probability.compareTo(approveThreshold) < 0) {
            decisionType = DecisionType.APPROVED;
            reason = "Low risk score (" + probability + "). Auto-approved.";
        } else if (probability.compareTo(rejectThreshold) > 0) {
            decisionType = DecisionType.REJECTED;
            reason = "High risk score (" + probability + "). Auto-rejected.";
        } else {
            decisionType = DecisionType.MANUAL_REVIEW;
            reason = "Medium risk score (" + probability + "). Requires manual review.";
        }

        // Save the decision
        Decision decision = new Decision();
        decision.setLoan(loan);
        decision.setDecision(decisionType);
        decision.setReason(reason);
        decisionRepository.save(decision);

        // Update loan status based on decision
        if (decisionType == DecisionType.APPROVED) {
            loan.setStatus(LoanStatus.APPROVED);
        } else if (decisionType == DecisionType.REJECTED) {
            loan.setStatus(LoanStatus.REJECTED);
        }
        // MANUAL_REVIEW leaves status as UNDER_REVIEW
        loanRepository.save(loan);

        return decision;
    }

    // Get decision for a loan — used by the REST endpoint
    public Decision getByLoanId(UUID loanId) {
        return decisionRepository.findByLoanId(loanId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Decision not found for loan: " + loanId));
    }
}