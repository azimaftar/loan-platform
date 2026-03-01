package com.azimali.loanplatform.modules.users;

import com.azimali.loanplatform.common.exception.BadRequestException;
import com.azimali.loanplatform.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    // Spring automatically injects UserRepository here
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Find a user by ID and convert to DTO
    public UserDTO getById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new UserDTO(user);
    }

    // Update username and/or profileInfo
    public UserDTO updateProfile(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Only update username if it was provided AND it's different from current
        if (request.getUsername() != null) {
            if (userRepository.existsByUsername(request.getUsername())
                    && !user.getUsername().equals(request.getUsername())) {
                throw new BadRequestException("Username is already taken");
            }
            user.setUsername(request.getUsername());
        }

        // Only update profileInfo if it was provided
        if (request.getProfileInfo() != null) {
            user.setProfileInfo(request.getProfileInfo());
        }

        userRepository.save(user);
        return new UserDTO(user);
    }

    // Soft delete — never hard delete users
    public void deactivateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(false);
        userRepository.save(user);
    }
}