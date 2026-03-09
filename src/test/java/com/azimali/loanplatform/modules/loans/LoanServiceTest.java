package com.azimali.loanplatform.modules.loans;

import com.azimali.loanplatform.common.exception.BadRequestException;
import com.azimali.loanplatform.common.exception.ResourceNotFoundException;
import com.azimali.loanplatform.common.exception.UnauthorizedException;
import com.azimali.loanplatform.modules.audit.AuditService;
import com.azimali.loanplatform.modules.decisions.DecisionService;
import com.azimali.loanplatform.modules.risk.RiskScore;
import com.azimali.loanplatform.modules.risk.RiskService;
import com.azimali.loanplatform.modules.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private RiskService riskService;
    @Mock private DecisionService decisionService;
    @Mock private AuditService auditService;

    @InjectMocks
    private LoanService loanService;

    private User mockUser;
    private Loan mockLoan;
    private UUID loanId;

    @BeforeEach
    void setUp() {
        loanId = UUID.randomUUID();

        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setUsername("azimali");
        mockUser.setRole(User.Role.USER);

        mockLoan = new Loan();
        mockLoan.setId(loanId);          // ← add this line
        mockLoan.setUser(mockUser);
        mockLoan.setAmount(new BigDecimal("5000"));
        mockLoan.setPurpose("Home renovation");
        mockLoan.setStatus(LoanStatus.PENDING);
    }

    // ─── CREATE TESTS ────────────────────────────────────────────────────────

    @Test
    void createLoan_withValidData_shouldReturnLoanDTO() {
        // Arrange
        CreateLoanRequest request = new CreateLoanRequest();
        request.setAmount(new BigDecimal("5000"));
        request.setPurpose("Home renovation");

        // Return mockLoan which already has an ID set in setUp()
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan loan = invocation.getArgument(0);
            loan.setId(loanId);
            loan.setStatus(LoanStatus.PENDING); // ← add this line
            return loan;
        });

        // Act
        LoanDTO result = loanService.createLoan(request, mockUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(loanRepository).save(any(Loan.class));
        verify(auditService).log(any(), eq("LOAN_CREATED"), eq("LOAN"), any(), any());
    }

    // ─── UPDATE TESTS ────────────────────────────────────────────────────────

    @Test
    void updateLoan_whilePending_shouldSucceed() {
        // Arrange
        UpdateLoanRequest request = new UpdateLoanRequest();
        request.setAmount(new BigDecimal("6000"));
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(mockLoan));
        when(loanRepository.save(any())).thenReturn(mockLoan);

        // Act
        LoanDTO result = loanService.updateLoan(loanId, request, mockUser);

        // Assert
        assertThat(result).isNotNull();
        verify(loanRepository).save(any());
    }

    @Test
    void updateLoan_afterSubmission_shouldThrowBadRequestException() {
        // Arrange
        mockLoan.setStatus(LoanStatus.UNDER_REVIEW);
        UpdateLoanRequest request = new UpdateLoanRequest();
        request.setAmount(new BigDecimal("6000"));
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(mockLoan));

        // Act & Assert
        assertThatThrownBy(() -> loanService.updateLoan(loanId, request, mockUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only PENDING loans can be updated");
    }

    @Test
    void updateLoan_byDifferentUser_shouldThrowUnauthorizedException() {
        // Arrange
        User anotherUser = new User();
        anotherUser.setId(UUID.randomUUID());
        anotherUser.setRole(User.Role.USER);
        UpdateLoanRequest request = new UpdateLoanRequest();
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(mockLoan));

        // Act & Assert
        assertThatThrownBy(() -> loanService.updateLoan(loanId, request, anotherUser))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ─── SUBMIT TESTS ────────────────────────────────────────────────────────

    @Test
    void submitLoan_whilePending_shouldTriggerRiskAndDecision() {
        // Arrange
        when(loanRepository.findById(loanId))
                .thenReturn(Optional.of(mockLoan));
        when(loanRepository.save(any())).thenReturn(mockLoan);
        when(riskService.computeAndSave(any())).thenReturn(new RiskScore());

        // Act
        loanService.submitLoan(loanId, mockUser);

        // Assert
        verify(riskService).computeAndSave(any());
        verify(decisionService).decide(any(), any());
        verify(auditService, atLeastOnce()).log(any(), any(), any(), any(), any());
    }

    @Test
    void submitLoan_alreadySubmitted_shouldThrowBadRequestException() {
        // Arrange
        mockLoan.setStatus(LoanStatus.UNDER_REVIEW);
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(mockLoan));

        // Act & Assert
        assertThatThrownBy(() -> loanService.submitLoan(loanId, mockUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot transition loan from UNDER_REVIEW to UNDER_REVIEW");
    }

    // ─── STATUS TRANSITION TESTS ─────────────────────────────────────────────

    @Test
    void markAsPaid_approvedLoan_shouldSucceed() {
        // Arrange
        mockLoan.setStatus(LoanStatus.APPROVED);
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(mockLoan));
        when(loanRepository.save(any())).thenReturn(mockLoan);

        // Act
        LoanDTO result = loanService.markAsPaid(loanId, mockUser);

        // Assert
        assertThat(result).isNotNull();
        verify(loanRepository).save(any());
    }

    @Test
    void markAsPaid_pendingLoan_shouldThrowBadRequestException() {
        // Arrange — loan is still PENDING
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(mockLoan));

        // Act & Assert
        assertThatThrownBy(() -> loanService.markAsPaid(loanId, mockUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cannot transition loan from PENDING to PAID");
    }

    @Test
    void getLoanById_nonExistentLoan_shouldThrowResourceNotFoundException() {
        // Arrange
        when(loanRepository.findById(any())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> loanService.getLoanById(UUID.randomUUID(), mockUser))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Loan not found");
    }
}