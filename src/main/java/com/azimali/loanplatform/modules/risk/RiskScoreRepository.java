package com.azimali.loanplatform.modules.risk;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RiskScoreRepository extends JpaRepository<RiskScore, UUID> {
    Optional<RiskScore> findByLoanId(UUID loanId);
}