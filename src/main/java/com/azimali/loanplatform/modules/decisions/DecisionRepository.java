package com.azimali.loanplatform.modules.decisions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DecisionRepository extends JpaRepository<Decision, UUID> {
    Optional<Decision> findByLoanId(UUID loanId);
}