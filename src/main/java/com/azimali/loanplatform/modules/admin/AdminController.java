package com.azimali.loanplatform.modules.admin;

import com.azimali.loanplatform.common.ApiResponse;
import com.azimali.loanplatform.modules.users.User;
import com.azimali.loanplatform.modules.users.UserDTO;
import com.azimali.loanplatform.modules.users.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin only endpoints")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    @Operation(summary = "List all users", description = "Admin only — returns paginated list of all users")
    public ResponseEntity<ApiResponse<Page<UserDTO>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserDTO> users = userRepository.findAll(pageable).map(UserDTO::new);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Update user role", description = "Admin only — changes a user's role")
    public ResponseEntity<ApiResponse<UserDTO>> updateUserRole(
            @PathVariable java.util.UUID id,
            @RequestParam User.Role role,
            com.azimali.loanplatform.modules.users.UserService userService) {
        // Fetch user, update role, save
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.azimali.loanplatform.common.exception
                        .ResourceNotFoundException("User not found"));
        user.setRole(role);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(new UserDTO(user), "Role updated successfully"));
    }
}