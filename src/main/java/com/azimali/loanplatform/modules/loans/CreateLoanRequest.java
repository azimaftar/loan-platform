package com.azimali.loanplatform.modules.loans;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateLoanRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100.00", message = "Minimum loan amount is 100")
    private BigDecimal amount;

    @NotBlank(message = "Purpose is required")
    private String purpose;
}