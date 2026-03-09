package com.azimali.loanplatform.modules.loans;

import com.azimali.loanplatform.common.exception.BadRequestException;
import com.azimali.loanplatform.common.exception.ResourceNotFoundException;
import com.azimali.loanplatform.common.exception.UnauthorizedException;
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

    public LoanService(LoanRepository loanRepository,
                       @Lazy RiskService riskService,
                       @Lazy DecisionService decisionService) {
        this.loanRepository = loanRepository;
        this.riskService = riskService;
        this.decisionService = decisionService;
    }

    public LoanDTO createLoan(CreateLoanRequest request, User currentUser) {
        Loan loan = new Loan();
        loan.setUser(currentUser);
        loan.setAmount(request.getAmount());
        loan.setPurpose(request.getPurpose());
        loanRepository.save(loan);
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
        return new LoanDTO(loan);
    }

    // Updated — now triggers risk scoring and decision engine
    public LoanDTO submitLoan(UUID id, User currentUser) {
        Loan loan = findLoanOrThrow(id);

        if (!loan.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not have access to this loan");
        }

        transitionStatus(loan, LoanStatus.UNDER_REVIEW);
        loanRepository.save(loan);
        System.out.println("DEBUG: Loan saved as UNDER_REVIEW");

        // Trigger risk scoring
        System.out.println("DEBUG: About to compute risk score");
        RiskScore riskScore = riskService.computeAndSave(loan);
        System.out.println("DEBUG: Risk score computed: " + riskScore.getProbability());

        // Trigger decision engine
        System.out.println("DEBUG: About to make decision");
        decisionService.decide(loan, riskScore);
        System.out.println("DEBUG: Decision made");

        // Reload loan to get updated status after decision
        loan = findLoanOrThrow(id);
        return new LoanDTO(loan);
    }

    public LoanDTO approveLoan(UUID id) {
        Loan loan = findLoanOrThrow(id);
        transitionStatus(loan, LoanStatus.APPROVED);
        loanRepository.save(loan);
        return new LoanDTO(loan);
    }

    public LoanDTO rejectLoan(UUID id) {
        Loan loan = findLoanOrThrow(id);
        transitionStatus(loan, LoanStatus.REJECTED);
        loanRepository.save(loan);
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