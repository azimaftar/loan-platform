package com.azimali.loanplatform.modules.loans;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateLoanRequest {

    @DecimalMin(value = "100.00", message = "Minimum loan amount is 100")
    private BigDecimal amount;

    private String purpose;
}