package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.AdminCreateRequest;
import com.example.DevConnect.dto.response.UserResponse;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.exception.DuplicateResourceException;
import com.example.DevConnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        // Mapped to a DTO: the entity carries the password hash.
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    /**
     * Creates a fresh admin account. Returning a DTO instead of a ResponseEntity keeps HTTP
     * concerns in the controller, and failures now surface as real status codes through the
     * global handler instead of a blanket "Something went wrong".
     */
    @Transactional
    public UserResponse createAdmin(AdminCreateRequest request) {
        if (userRepository.existsByUserName(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        User user = new User();
        user.setUserName(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(List.of("ADMIN", "USER"));

        return UserResponse.from(userRepository.save(user));
    }
}
