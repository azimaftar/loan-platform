package com.azimali.loanplatform.modules.risk;

import com.azimali.loanplatform.modules.loans.Loan;
import com.azimali.loanplatform.modules.loans.LoanRepository;
import com.azimali.loanplatform.modules.loans.LoanStatus;
import com.azimali.loanplatform.modules.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskScoringEngineTest {

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private RiskScoringEngine riskScoringEngine;

    private User mockUser;
    private Loan mockLoan;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setUsername("azimali");
        mockUser.setRole(User.Role.USER);

        mockLoan = new Loan();
        mockLoan.setUser(mockUser);
    }

    @Test
    void score_smallLoanNewUser_shouldReturnLowRisk() {
        // Arrange — small loan, no history
        mockLoan.setAmount(new BigDecimal("1000"));
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.REJECTED)))
                .thenReturn(0L);
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.PAID)))
                .thenReturn(0L);
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.PENDING)))
                .thenReturn(0L);
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.APPROVED)))
                .thenReturn(0L);

        // Act
        RiskScoringEngine.ScoringResult result = riskScoringEngine.score(mockLoan);

        // Assert — base 0.30 + new user 0.10 = 0.40
        assertThat(result.probability())
                .isEqualByComparingTo(new BigDecimal("0.4000"));
        assertThat(result.factors()).contains("base_score");
        assertThat(result.factors()).contains("new_user_no_history");
    }

    @Test
    void score_largeLoanNoHistory_shouldReturnHighRisk() {
        // Arrange — very large loan
        mockLoan.setAmount(new BigDecimal("150000"));
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.REJECTED)))
                .thenReturn(0L);
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.PAID)))
                .thenReturn(0L);
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.PENDING)))
                .thenReturn(0L);
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.APPROVED)))
                .thenReturn(0L);

        // Act
        RiskScoringEngine.ScoringResult result = riskScoringEngine.score(mockLoan);

        // Assert — base 0.30 + very high amount 0.35 + new user 0.10 = 0.75
        assertThat(result.probability())
                .isEqualByComparingTo(new BigDecimal("0.7500"));
        assertThat(result.factors()).contains("amount_very_high");
    }

    @Test
    void score_withPreviousRejections_shouldIncreaseRisk() {
        // Arrange
        mockLoan.setAmount(new BigDecimal("5000"));
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.REJECTED)))
                .thenReturn(2L);
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.PAID)))
                .thenReturn(0L);
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.PENDING)))
                .thenReturn(1L);
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.APPROVED)))
                .thenReturn(0L);

        // Act
        RiskScoringEngine.ScoringResult result = riskScoringEngine.score(mockLoan);

        // Assert — should include rejection penalty
        assertThat(result.factors()).contains("previous_rejections");
        assertThat(result.probability()).isGreaterThan(new BigDecimal("0.30"));
    }

    @Test
    void score_withGoodRepaymentHistory_shouldReduceRisk() {
        // Arrange
        mockLoan.setAmount(new BigDecimal("5000"));
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.REJECTED)))
                .thenReturn(0L);
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.PAID)))
                .thenReturn(2L);
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.PENDING)))
                .thenReturn(1L);
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.APPROVED)))
                .thenReturn(0L);

        // Act
        RiskScoringEngine.ScoringResult result = riskScoringEngine.score(mockLoan);

        // Assert — should include repayment reduction
        assertThat(result.factors()).contains("repaid_loans");
        assertThat(result.probability()).isLessThan(new BigDecimal("0.50"));
    }

    @Test
    void score_shouldNeverExceedOne() {
        // Arrange — worst case scenario
        mockLoan.setAmount(new BigDecimal("200000"));
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.REJECTED)))
                .thenReturn(5L);
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.PAID)))
                .thenReturn(0L);
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.PENDING)))
                .thenReturn(0L);
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.APPROVED)))
                .thenReturn(0L);

        // Act
        RiskScoringEngine.ScoringResult result = riskScoringEngine.score(mockLoan);

        // Assert
        assertThat(result.probability())
                .isLessThanOrEqualTo(new BigDecimal("1.0000"));
    }

    @Test
    void score_shouldNeverGoBelowZero() {
        // Arrange — best case scenario
        mockLoan.setAmount(new BigDecimal("100"));
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.REJECTED)))
                .thenReturn(0L);
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.PAID)))
                .thenReturn(10L);
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.PENDING)))
                .thenReturn(5L);
        when(loanRepository.countByUserIdAndStatus(any(), eq(LoanStatus.APPROVED)))
                .thenReturn(0L);

        // Act
        RiskScoringEngine.ScoringResult result = riskScoringEngine.score(mockLoan);

        // Assert
        assertThat(result.probability())
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }
}