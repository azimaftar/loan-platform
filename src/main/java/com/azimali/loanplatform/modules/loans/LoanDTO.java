package com.azimali.loanplatform.modules.loans;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class LoanDTO {

    private UUID id;
    private UUID userId;
    private String username;
    private BigDecimal amount;
    private String purpose;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LoanDTO(Loan loan) {
        this.id = loan.getId();
        this.userId = loan.getUser().getId();
        this.username = loan.getUser().getUsername();
        this.amount = loan.getAmount();
        this.purpose = loan.getPurpose();
        this.status = loan.getStatus().name();
        this.notes = loan.getNotes();
        this.createdAt = loan.getCreatedAt();
        this.updatedAt = loan.getUpdatedAt();
    }

}