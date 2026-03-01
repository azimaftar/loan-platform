package com.azimali.loanplatform.modules.users;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserDTO {

    private UUID id;
    private String username;
    private String email;
    private String role;
    private String profileInfo;
    private boolean isActive;
    private LocalDateTime createdAt;

    // Takes a User entity and copies only the safe fields into this DTO
    public UserDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.role = user.getRole().name();
        this.profileInfo = user.getProfileInfo();
        this.isActive = user.isActive();
        this.createdAt = user.getCreatedAt();
    }
}