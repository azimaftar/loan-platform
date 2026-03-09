package com.azimali.loanplatform.modules.risk;

import com.azimali.loanplatform.modules.loans.Loan;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "risk_scores")
public class RiskScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // One risk score per loan
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false, unique = true)
    private Loan loan;

    // The calculated probability score 0.00 - 1.00
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal probability;

    // JSON string storing which rules fired and their contribution
    @Column(columnDefinition = "TEXT")
    private String factors;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}