package com.azimali.loanplatform.modules.users;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Size(max = 500, message = "Profile info cannot exceed 500 characters")
    private String profileInfo;
}