package com.example.DevConnect.controller;

import com.example.DevConnect.dto.request.LoginRequest;
import com.example.DevConnect.dto.request.RegisterRequest;
import com.example.DevConnect.dto.response.ApiResponse;
import com.example.DevConnect.exception.MethodArgumentNotValidException;
import com.example.DevConnect.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            String message = authService.register(request);
            return ResponseEntity.ok(ApiResponse.success(message, null));
        }catch (MethodArgumentNotValidException m){
            throw new MethodArgumentNotValidException("Please fill the proper details in the form.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Login successful!", authService.login(request)));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(ApiResponse.error("Invalid credentials"));
        }
    }
}
