package com.azimali.loanplatform.modules.loans;

import com.azimali.loanplatform.common.ApiResponse;
import com.azimali.loanplatform.modules.users.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/loans")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Loans", description = "Loan application lifecycle management")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    @Operation(summary = "Create loan application", description = "Creates a new loan application with PENDING status")
    public ResponseEntity<ApiResponse<LoanDTO>> createLoan(
            @Valid @RequestBody CreateLoanRequest request,
            @AuthenticationPrincipal User currentUser) {
        LoanDTO dto = loanService.createLoan(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(dto, "Loan application created"));
    }

    @GetMapping
    @Operation(summary = "Get all loans", description = "Users see their own loans. Loan officers and admins see all loans")
    public ResponseEntity<ApiResponse<Page<LoanDTO>>> getAllLoans(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<LoanDTO> loans = loanService.getAllLoans(currentUser, pageable);
        return ResponseEntity.ok(ApiResponse.success(loans));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan by ID", description = "Users can only view their own loans")
    public ResponseEntity<ApiResponse<LoanDTO>> getLoanById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        LoanDTO dto = loanService.getLoanById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update loan", description = "Only allowed when loan status is PENDING")
    public ResponseEntity<ApiResponse<LoanDTO>> updateLoan(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLoanRequest request,
            @AuthenticationPrincipal User currentUser) {
        LoanDTO dto = loanService.updateLoan(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(dto, "Loan updated successfully"));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit loan for review", description = "Triggers automatic risk scoring and decision engine")
    public ResponseEntity<ApiResponse<LoanDTO>> submitLoan(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        LoanDTO dto = loanService.submitLoan(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(dto, "Loan submitted for review"));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
    @Operation(summary = "Manually approve loan", description = "Loan officer or admin only — manually approves a loan in UNDER_REVIEW")
    public ResponseEntity<ApiResponse<LoanDTO>> approveLoan(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        LoanDTO dto = loanService.approveLoan(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(dto, "Loan approved"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
    @Operation(summary = "Manually reject loan", description = "Loan officer or admin only — manually rejects a loan in UNDER_REVIEW")
    public ResponseEntity<ApiResponse<LoanDTO>> rejectLoan(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        LoanDTO dto = loanService.rejectLoan(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(dto, "Loan rejected"));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Mark loan as paid", description = "Marks an APPROVED loan as PAID — completes the loan lifecycle")
    public ResponseEntity<ApiResponse<LoanDTO>> markAsPaid(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        LoanDTO dto = loanService.markAsPaid(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(dto, "Loan marked as paid"));
    }
}