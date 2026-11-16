package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.LoginRequest;
import com.example.DevConnect.dto.request.RegisterRequest;
import com.example.DevConnect.dto.response.AuthResponse;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.repository.UserRepository;
import com.example.DevConnect.support.TestData;
import com.example.DevConnect.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private static RegisterRequest request(String username, String email, String role) {
        return new RegisterRequest(username, email, "Secret@123", role);
    }

    @Test
    void registerSavesTrimmedUserWithEncodedPasswordAndUppercaseRole() {
        when(userRepository.findByUserName("harsh")).thenReturn(null);
        when(userRepository.findByEmail("harsh@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Secret@123")).thenReturn("ENCODED");

        String message = authService.register(request("  harsh ", " harsh@gmail.com ", "developer"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("harsh", saved.getValue().getUserName());
        assertEquals("harsh@gmail.com", saved.getValue().getEmail());
        assertEquals("ENCODED", saved.getValue().getPassword());
        assertEquals(List.of("DEVELOPER"), saved.getValue().getRole());
        assertEquals("User registered successfully as DEVELOPER! Please log in.", message);
    }

    @Test
    void registerSendsWelcomeEmail() {
        when(userRepository.findByEmail("harsh@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("ENCODED");

        authService.register(request("harsh", "harsh@gmail.com", "RECRUITER"));

        verify(emailService).sendWelcomeEmail("harsh@gmail.com", "harsh");
    }

    @Test
    void registerRejectsTakenUsername() {
        when(userRepository.findByUserName("harsh")).thenReturn(new User());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(request("harsh", "harsh@gmail.com", "DEVELOPER")));

        assertEquals("Username already exists", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsTakenEmail() {
        when(userRepository.findByEmail("harsh@gmail.com")).thenReturn(Optional.of(new User()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(request("harsh", "harsh@gmail.com", "DEVELOPER")));

        assertEquals("Email already exists", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsNonGmailAddress() {
        when(userRepository.findByEmail("harsh@yahoo.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(request("harsh", "harsh@yahoo.com", "DEVELOPER")));

        assertEquals("Invalid email address", ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendWelcomeEmail(any(), any());
    }

    @Test
    void loginReturnsTokenEmailAndRoles() {
        User user = TestData.user(1L, "harsh@gmail.com", "DEVELOPER");
        UserPrincipal principal = new UserPrincipal(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtUtil.generateToken(principal)).thenReturn("jwt-token");
        when(userRepository.findByEmail("harsh@gmail.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(new LoginRequest("harsh@gmail.com", "Secret@123"));

        assertEquals("jwt-token", response.getToken());
        assertEquals("harsh@gmail.com", response.getEmail());
        assertEquals(List.of("DEVELOPER"), response.getRole());
    }

    @Test
    void loginPassesCredentialsToAuthenticationManager() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("harsh@gmail.com", "wrong")));

        ArgumentCaptor<Authentication> attempt = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(attempt.capture());
        assertEquals("harsh@gmail.com", attempt.getValue().getPrincipal());
        assertEquals("wrong", attempt.getValue().getCredentials());
        verify(jwtUtil, never()).generateToken(any());
    }
}
