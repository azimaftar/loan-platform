package com.azimali.loanplatform.modules.loans;

import com.azimali.loanplatform.common.ApiResponse;
import com.azimali.loanplatform.modules.users.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    // POST /api/loans — any authenticated user
    @PostMapping
    public ResponseEntity<ApiResponse<LoanDTO>> createLoan(
            @Valid @RequestBody CreateLoanRequest request,
            @AuthenticationPrincipal User currentUser) {
        LoanDTO dto = loanService.createLoan(request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(dto, "Loan application created"));
    }

    // GET /api/loans — paginated, role-aware
    @GetMapping
    public ResponseEntity<ApiResponse<Page<LoanDTO>>> getAllLoans(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<LoanDTO> loans = loanService.getAllLoans(currentUser, pageable);
        return ResponseEntity.ok(ApiResponse.success(loans));
    }

    // GET /api/loans/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanDTO>> getLoanById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        LoanDTO dto = loanService.getLoanById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    // PUT /api/loans/{id} — only if PENDING
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanDTO>> updateLoan(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLoanRequest request,
            @AuthenticationPrincipal User currentUser) {
        LoanDTO dto = loanService.updateLoan(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(dto, "Loan updated successfully"));
    }

    // POST /api/loans/{id}/submit — user submits for review
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<LoanDTO>> submitLoan(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        LoanDTO dto = loanService.submitLoan(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(dto, "Loan submitted for review"));
    }

    // POST /api/loans/{id}/approve — LOAN_OFFICER or ADMIN only
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LoanDTO>> approveLoan(@PathVariable UUID id) {
        LoanDTO dto = loanService.approveLoan(id);
        return ResponseEntity.ok(ApiResponse.success(dto, "Loan approved"));
    }

    // POST /api/loans/{id}/reject — LOAN_OFFICER or ADMIN only
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LoanDTO>> rejectLoan(@PathVariable UUID id) {
        LoanDTO dto = loanService.rejectLoan(id);
        return ResponseEntity.ok(ApiResponse.success(dto, "Loan rejected"));
    }
}