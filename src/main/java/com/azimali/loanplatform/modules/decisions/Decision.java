package com.azimali.loanplatform.modules.decisions;

import com.azimali.loanplatform.modules.loans.Loan;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "decisions")
public class Decision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false, unique = true)
    private Loan loan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisionType decision;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}