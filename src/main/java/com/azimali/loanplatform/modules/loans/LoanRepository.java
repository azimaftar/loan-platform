package com.azimali.loanplatform.modules.loans;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    // Find all loans for a specific user — paginated
    Page<Loan> findByUserId(UUID userId, Pageable pageable);

    // Find all loans by status — paginated
    Page<Loan> findByStatus(LoanStatus status, Pageable pageable);

    // Count loans by user and status — used in risk scoring later
    long countByUserIdAndStatus(UUID userId, LoanStatus status);
}