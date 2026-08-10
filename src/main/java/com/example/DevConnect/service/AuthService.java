package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.LoginRequest;
import com.example.DevConnect.dto.request.RegisterRequest;
import com.example.DevConnect.dto.response.AuthResponse;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.exception.BadRequestException;
import com.example.DevConnect.exception.DuplicateResourceException;
import com.example.DevConnect.repository.UserRepository;
import com.example.DevConnect.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private EmailService emailService;

    @Transactional
    public String register(RegisterRequest request) {

        String email = request.getEmail();

        if (!email.endsWith("@gmail.com")) {
            throw new BadRequestException("Only @gmail.com email addresses are accepted");
        }

        if (userRepository.existsByUserName(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = new User();
        user.setUserName(request.getUsername());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(List.of(request.getRole().toUpperCase()));

        userRepository.save(user);
        emailService.sendWelcomeEmail(user.getEmail(), user.getUserName());

        return "User registered successfully as " + user.getRole().get(0) + "! Please log in.";
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
