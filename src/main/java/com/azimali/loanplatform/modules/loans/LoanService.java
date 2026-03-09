package com.azimali.loanplatform.modules.loans;

import com.azimali.loanplatform.common.exception.BadRequestException;
import com.azimali.loanplatform.common.exception.ResourceNotFoundException;
import com.azimali.loanplatform.common.exception.UnauthorizedException;
import com.azimali.loanplatform.modules.audit.AuditService;
import com.azimali.loanplatform.modules.decisions.DecisionService;
import com.azimali.loanplatform.modules.risk.RiskScore;
import com.azimali.loanplatform.modules.risk.RiskService;
import com.azimali.loanplatform.modules.users.User;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final RiskService riskService;
    private final DecisionService decisionService;
    private final AuditService auditService;

    public LoanService(LoanRepository loanRepository,
                       @Lazy RiskService riskService,
                       @Lazy DecisionService decisionService,
                       AuditService auditService) {
        this.loanRepository = loanRepository;
        this.riskService = riskService;
        this.decisionService = decisionService;
        this.auditService = auditService;
    }

    public LoanDTO createLoan(CreateLoanRequest request, User currentUser) {
        Loan loan = new Loan();
        loan.setUser(currentUser);
        loan.setAmount(request.getAmount());
        loan.setPurpose(request.getPurpose());
        loanRepository.save(loan);

        auditService.log(currentUser, "LOAN_CREATED", "LOAN",
                loan.getId().toString(),
                "Loan created for amount: " + loan.getAmount());

        return new LoanDTO(loan);
    }

    public Page<LoanDTO> getAllLoans(User currentUser, Pageable pageable) {
        if (currentUser.getRole() == User.Role.USER) {
            return loanRepository.findByUserId(currentUser.getId(), pageable)
                    .map(LoanDTO::new);
        }
        return loanRepository.findAll(pageable).map(LoanDTO::new);
    }

    public LoanDTO getLoanById(UUID id, User currentUser) {
        Loan loan = findLoanOrThrow(id);
        if (currentUser.getRole() == User.Role.USER &&
                !loan.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not have access to this loan");
        }
        return new LoanDTO(loan);
    }

    public LoanDTO updateLoan(UUID id, UpdateLoanRequest request, User currentUser) {
        Loan loan = findLoanOrThrow(id);
        if (!loan.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not have access to this loan");
        }
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new BadRequestException("Only PENDING loans can be updated");
        }
        if (request.getAmount() != null) loan.setAmount(request.getAmount());
        if (request.getPurpose() != null) loan.setPurpose(request.getPurpose());
        loanRepository.save(loan);

        auditService.log(currentUser, "LOAN_UPDATED", "LOAN",
                loan.getId().toString(),
                "Loan updated");

        return new LoanDTO(loan);
    }

    public LoanDTO submitLoan(UUID id, User currentUser) {
        Loan loan = findLoanOrThrow(id);

        if (!loan.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not have access to this loan");
        }

        transitionStatus(loan, LoanStatus.UNDER_REVIEW);
        loanRepository.save(loan);

        auditService.log(currentUser, "LOAN_SUBMITTED", "LOAN",
                loan.getId().toString(),
                "Loan submitted for review");

        RiskScore riskScore = riskService.computeAndSave(loan);
        decisionService.decide(loan, riskScore);

        loan = findLoanOrThrow(id);

        auditService.log(currentUser, "LOAN_DECISION_MADE", "LOAN",
                loan.getId().toString(),
                "Loan decision: " + loan.getStatus());

        return new LoanDTO(loan);
    }

    public LoanDTO approveLoan(UUID id, User currentUser) {
        Loan loan = findLoanOrThrow(id);
        transitionStatus(loan, LoanStatus.APPROVED);
        loanRepository.save(loan);

        auditService.log(currentUser, "LOAN_MANUALLY_APPROVED", "LOAN",
                loan.getId().toString(),
                "Loan manually approved by: " + currentUser.getUsername());

        return new LoanDTO(loan);
    }

    public LoanDTO rejectLoan(UUID id, User currentUser) {
        Loan loan = findLoanOrThrow(id);
        transitionStatus(loan, LoanStatus.REJECTED);
        loanRepository.save(loan);

        auditService.log(currentUser, "LOAN_MANUALLY_REJECTED", "LOAN",
                loan.getId().toString(),
                "Loan manually rejected by: " + currentUser.getUsername());

        return new LoanDTO(loan);
    }

    // New — mark approved loan as paid
    public LoanDTO markAsPaid(UUID id, User currentUser) {
        Loan loan = findLoanOrThrow(id);

        if (!loan.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not have access to this loan");
        }

        transitionStatus(loan, LoanStatus.PAID);
        loanRepository.save(loan);

        auditService.log(currentUser, "LOAN_PAID", "LOAN",
                loan.getId().toString(),
                "Loan marked as paid by: " + currentUser.getUsername());

        return new LoanDTO(loan);
    }

    private Loan findLoanOrThrow(UUID id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
    }

    private void transitionStatus(Loan loan, LoanStatus newStatus) {
        if (!canTransitionTo(loan.getStatus(), newStatus)) {
            throw new BadRequestException(
                    "Cannot transition loan from " + loan.getStatus() + " to " + newStatus
            );
        }
        loan.setStatus(newStatus);
    }

    private boolean canTransitionTo(LoanStatus current, LoanStatus next) {
        return switch (current) {
            case PENDING -> next == LoanStatus.UNDER_REVIEW;
            case UNDER_REVIEW -> next == LoanStatus.APPROVED || next == LoanStatus.REJECTED;
            case APPROVED -> next == LoanStatus.PAID;
            default -> false;
        };
    }
}