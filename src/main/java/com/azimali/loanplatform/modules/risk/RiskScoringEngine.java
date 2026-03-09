package com.azimali.loanplatform.modules.risk;

import com.azimali.loanplatform.modules.loans.Loan;
import com.azimali.loanplatform.modules.loans.LoanRepository;
import com.azimali.loanplatform.modules.loans.LoanStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class RiskScoringEngine {

    private final LoanRepository loanRepository;

    public RiskScoringEngine(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    public ScoringResult score(Loan loan) {
        double score = 0.0;
        List<String> factors = new ArrayList<>();

        // Base score — everyone starts here
        score += 0.30;
        factors.add("base_score: +0.30");

        // Rule 1 — High loan amount
        if (loan.getAmount().compareTo(new BigDecimal("100000")) > 0) {
            score += 0.35;
            factors.add("amount_very_high (>100k): +0.35");
        } else if (loan.getAmount().compareTo(new BigDecimal("50000")) > 0) {
            score += 0.20;
            factors.add("amount_high (>50k): +0.20");
        }

        // Rule 2 — Previous rejected loans
        long rejectedLoans = loanRepository.countByUserIdAndStatus(
                loan.getUser().getId(), LoanStatus.REJECTED);
        if (rejectedLoans > 0) {
            score += 0.20;
            factors.add("previous_rejections (" + rejectedLoans + "): +0.20");
        }

        // Rule 3 — Previously repaid loans (good history)
        long paidLoans = loanRepository.countByUserIdAndStatus(
                loan.getUser().getId(), LoanStatus.PAID);
        if (paidLoans > 0) {
            double reduction = Math.min(paidLoans * 0.15, 0.30);
            score -= reduction;
            factors.add("repaid_loans (" + paidLoans + "): -" + reduction);
        }

        // Rule 4 — New user with no history
        long totalLoans = loanRepository.countByUserIdAndStatus(
                loan.getUser().getId(), LoanStatus.PENDING)
                + loanRepository.countByUserIdAndStatus(
                loan.getUser().getId(), LoanStatus.APPROVED)
                + paidLoans + rejectedLoans;

        if (totalLoans <= 1) {
            score += 0.10;
            factors.add("new_user_no_history: +0.10");
        }

        // Cap score between 0.0 and 1.0
        score = Math.max(0.0, Math.min(1.0, score));

        return new ScoringResult(
                BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP),
                String.join(", ", factors)
        );
    }

    // Simple result object holding score and factors
    public record ScoringResult(BigDecimal probability, String factors) {}
}