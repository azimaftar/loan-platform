package com.azimali.loanplatform.modules.loans;

import com.azimali.loanplatform.common.exception.BadRequestException;
import com.azimali.loanplatform.common.exception.ResourceNotFoundException;
import com.azimali.loanplatform.common.exception.UnauthorizedException;
import com.azimali.loanplatform.modules.users.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LoanService {

    private final LoanRepository loanRepository;

    public LoanService(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    // Create a new loan — status defaults to PENDING via @PrePersist
    public LoanDTO createLoan(CreateLoanRequest request, User currentUser) {
        Loan loan = new Loan();
        loan.setUser(currentUser);
        loan.setAmount(request.getAmount());
        loan.setPurpose(request.getPurpose());
        loanRepository.save(loan);
        return new LoanDTO(loan);
    }

    // Get all loans — users see only their own, officers/admins see all
    public Page<LoanDTO> getAllLoans(User currentUser, Pageable pageable) {
        if (currentUser.getRole() == User.Role.USER) {
            return loanRepository.findByUserId(currentUser.getId(), pageable)
                    .map(LoanDTO::new);
        }
        return loanRepository.findAll(pageable).map(LoanDTO::new);
    }

    // Get single loan — users can only see their own
    public LoanDTO getLoanById(UUID id, User currentUser) {
        Loan loan = findLoanOrThrow(id);
        if (currentUser.getRole() == User.Role.USER &&
                !loan.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not have access to this loan");
        }
        return new LoanDTO(loan);
    }

    // Update loan — only allowed if status is PENDING
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

    // Submit loan for review — moves PENDING → UNDER_REVIEW
    public LoanDTO submitLoan(UUID id, User currentUser) {
        Loan loan = findLoanOrThrow(id);

        if (!loan.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not have access to this loan");
        }

        transitionStatus(loan, LoanStatus.UNDER_REVIEW);
        loanRepository.save(loan);
        return new LoanDTO(loan);
    }

    // Approve loan — LOAN_OFFICER/ADMIN only
    public LoanDTO approveLoan(UUID id) {
        Loan loan = findLoanOrThrow(id);
        transitionStatus(loan, LoanStatus.APPROVED);
        loanRepository.save(loan);
        return new LoanDTO(loan);
    }

    // Reject loan — LOAN_OFFICER/ADMIN only
    public LoanDTO rejectLoan(UUID id) {
        Loan loan = findLoanOrThrow(id);
        transitionStatus(loan, LoanStatus.REJECTED);
        loanRepository.save(loan);
        return new LoanDTO(loan);
    }

    // ─── PRIVATE HELPERS ────────────────────────────────────────────────────

    private Loan findLoanOrThrow(UUID id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
    }

    // Status transition guard — enforces valid status flow
    private void transitionStatus(Loan loan, LoanStatus newStatus) {
        if (!canTransitionTo(loan.getStatus(), newStatus)) {
            throw new BadRequestException(
                    "Cannot transition loan from " + loan.getStatus() + " to " + newStatus
            );
        }
        loan.setStatus(newStatus);
    }

    // Defines which transitions are allowed
    private boolean canTransitionTo(LoanStatus current, LoanStatus next) {
        return switch (current) {
            case PENDING -> next == LoanStatus.UNDER_REVIEW;
            case UNDER_REVIEW -> next == LoanStatus.APPROVED || next == LoanStatus.REJECTED;
            case APPROVED -> next == LoanStatus.PAID;
            default -> false; // REJECTED and PAID are terminal states
        };
    }
}