package com.azimali.loanplatform.modules.auth;

import com.azimali.loanplatform.common.exception.BadRequestException;
import java.util.UUID;
import com.azimali.loanplatform.modules.audit.AuditService;
import com.azimali.loanplatform.modules.users.User;
import com.azimali.loanplatform.modules.users.UserRepository;
import com.azimali.loanplatform.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User mockUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("azimali");
        registerRequest.setEmail("azim@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("azim@example.com");
        loginRequest.setPassword("password123");

        mockUser = new User();
        mockUser.setId(UUID.randomUUID()); // ← add this line
        mockUser.setUsername("azimali");
        mockUser.setEmail("azim@example.com");
        mockUser.setPasswordHash("hashedPassword");
        mockUser.setRole(User.Role.USER);
        mockUser.setActive(true);
    }

    // ─── REGISTER TESTS ─────────────────────────────────────────────────────

    @Test
    void register_withValidData_shouldReturnAuthResponse() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID()); // ← set ID on the user being saved
            return user;
        });
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("mockToken");

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mockToken");
        assertThat(response.getUsername()).isEqualTo("azimali");
        assertThat(response.getRole()).isEqualTo("USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_withExistingEmail_shouldThrowBadRequestException() {
        // Arrange
        when(userRepository.existsByEmail("azim@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email is already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_withExistingUsername_shouldThrowBadRequestException() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("azimali")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Username is already taken");

        verify(userRepository, never()).save(any());
    }

    // ─── LOGIN TESTS ─────────────────────────────────────────────────────────

    @Test
    void login_withValidCredentials_shouldReturnAuthResponse() {
        // Arrange
        when(userRepository.findByEmail("azim@example.com"))
                .thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password123", "hashedPassword"))
                .thenReturn(true);
        when(jwtTokenProvider.generateToken(any(User.class)))
                .thenReturn("mockToken");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mockToken");
    }

    @Test
    void login_withWrongPassword_shouldThrowBadRequestException() {
        // Arrange
        when(userRepository.findByEmail("azim@example.com"))
                .thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_withNonExistentEmail_shouldThrowBadRequestException() {
        // Arrange
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_withDeactivatedAccount_shouldThrowBadRequestException() {
        // Arrange
        mockUser.setActive(false);
        when(userRepository.findByEmail("azim@example.com"))
                .thenReturn(Optional.of(mockUser));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Account is deactivated");
    }
}