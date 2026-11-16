package com.example.DevConnect.controller;

import com.example.DevConnect.dto.request.LoginRequest;
import com.example.DevConnect.dto.request.RegisterRequest;
import com.example.DevConnect.dto.response.AuthResponse;
import com.example.DevConnect.service.AuthService;
import com.example.DevConnect.support.ControllerTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest extends ControllerTestSupport {

    @MockitoBean
    private AuthService authService;

    private static String register(String username, String email, String password, String role) {
        return """
                {"username":"%s","email":"%s","password":"%s","role":"%s"}
                """.formatted(username, email, password, role);
    }

    @Test
    void registerIsPublicAndReturnsTheServiceMessage() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn("User registered successfully as DEVELOPER! Please log in.");

        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(register("harsh", "harsh@gmail.com", "Secret@123", "DEVELOPER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully as DEVELOPER! Please log in."));
    }

    @Test
    void weakPasswordIsRejectedBeforeTheService() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(register("harsh", "harsh@gmail.com", "password", "DEVELOPER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Password must contain")));
        verify(authService, never()).register(any());
    }

    @Test
    void unknownRoleIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(register("harsh", "harsh@gmail.com", "Secret@123", "ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Role must be either DEVELOPER or RECRUITER")));
    }

    @Test
    void invalidEmailAndBlankUsernameAreBothReported() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(register("", "not-an-email", "Secret@123", "DEVELOPER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Username is required")))
                .andExpect(jsonPath("$.message").value(containsString("Invalid email format")));
    }

    @Test
    void businessRuleFailureIsA400WithTheReason() throws Exception {
        when(authService.register(any())).thenThrow(new RuntimeException("Email already exists"));

        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(register("harsh", "harsh@gmail.com", "Secret@123", "DEVELOPER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void loginReturnsTheToken() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(AuthResponse.builder().token("jwt").email("harsh@gmail.com").role(List.of("DEVELOPER")).build());

        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"email\":\"harsh@gmail.com\",\"password\":\"Secret@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt"))
                .andExpect(jsonPath("$.data.role[0]").value("DEVELOPER"));
    }

    @Test
    void wrongCredentialsAreA401WithoutDetail() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"email\":\"harsh@gmail.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void malformedJsonIsA400() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json").content("{not json"))
                .andExpect(status().isBadRequest());
    }
}
