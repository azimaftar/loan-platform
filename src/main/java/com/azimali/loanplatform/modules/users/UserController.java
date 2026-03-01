package com.azimali.loanplatform.modules.users;

import com.azimali.loanplatform.common.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // Spring automatically injects UserService here
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // GET /api/users/me — any authenticated user
    // @AuthenticationPrincipal gives you the logged-in User object directly
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getMyProfile(
            @AuthenticationPrincipal User currentUser) {
        UserDTO dto = userService.getById(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    // PUT /api/users/me — any authenticated user
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> updateMyProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateUserRequest request) {
        UserDTO dto = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(dto, "Profile updated successfully"));
    }

    // GET /api/users/{id} — ADMIN only
    // @PreAuthorize checks role before this method runs
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable UUID id) {
        UserDTO dto = userService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    // DELETE /api/users/{id} — ADMIN only, soft delete
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable UUID id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deactivated successfully"));
    }
}
