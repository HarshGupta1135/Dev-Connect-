package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.LoginRequest;
import com.example.DevConnect.dto.request.RegisterRequest;
import com.example.DevConnect.dto.response.AuthResponse;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.repository.UserRepository;
import com.example.DevConnect.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private EmailService emailService;

    public String register(RegisterRequest request) {

        // Trimmed before the uniqueness check, not after: the collation treats trailing
        // spaces as significant, so "harsh " would otherwise slip past as a new name.
        String username = request.getUsername() != null ? request.getUsername().trim() : null;
        String email = request.getEmail() != null ? request.getEmail().trim() : null;

        if (userRepository.findByUserName(username) != null) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUserName(username);
        user.setEmail(email);
        user.setPassword(request.getPassword());

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        if (!user.getEmail().endsWith("@gmail.com")) {
            throw new RuntimeException("Invalid email address");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        String chosenRole = request.getRole();
        user.setRole(List.of(chosenRole.toUpperCase()));
        
        userRepository.save(user);
        emailService.sendWelcomeEmail(user.resolveNotificationEmail(), user.getUserName());

        return "User registered successfully as " + user.getRole().get(0) + "! Please log in.";
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
