package com.example.DevConnect.controller;

import com.example.DevConnect.dto.request.LoginRequest;
import com.example.DevConnect.dto.request.RegisterRequest;
import com.example.DevConnect.dto.response.ApiResponse;
import com.example.DevConnect.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration and login")
public class AuthController {

    @Autowired
    private AuthService authService;

    /*
     * No try/catch here on purpose: GlobalExceptionHandler turns a duplicate user into 409, a
     * broken rule into 400 and bad credentials into 401. Catching everything locally reported
     * unrelated failures (a database outage, for example) as "Invalid credentials".
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user profile as a DEVELOPER or RECRUITER and triggers a welcome email.")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String message = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates the user credentials and returns a JWT access token.")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful!", authService.login(request)));
    }
}
