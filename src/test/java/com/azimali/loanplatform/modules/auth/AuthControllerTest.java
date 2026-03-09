package com.azimali.loanplatform.modules.auth;

import com.azimali.loanplatform.config.SecurityConfig;
import com.azimali.loanplatform.security.JwtAuthFilter;
import com.azimali.loanplatform.security.JwtTokenProvider;
import com.azimali.loanplatform.modules.users.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserRepository userRepository;

    @Test
    void register_withValidData_shouldReturn200() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("azimali");
        request.setEmail("azim@example.com");
        request.setPassword("password123");

        AuthResponse authResponse = new AuthResponse(
                "mockToken", "azimali", "azim@example.com", "USER");
        when(authService.register(any())).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mockToken"))
                .andExpect(jsonPath("$.data.username").value("azimali"));
    }

    @Test
    void register_withInvalidEmail_shouldReturn400() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("azimali");
        request.setEmail("not-an-email");
        request.setPassword("password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withShortPassword_shouldReturn400() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("azimali");
        request.setEmail("azim@example.com");
        request.setPassword("123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_withValidData_shouldReturn200() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("azim@example.com");
        request.setPassword("password123");

        AuthResponse authResponse = new AuthResponse(
                "mockToken", "azimali", "azim@example.com", "USER");
        when(authService.login(any())).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mockToken"));
    }

    @Test
    void login_withMissingEmail_shouldReturn400() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setPassword("password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
