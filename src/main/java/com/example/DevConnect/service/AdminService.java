package com.example.DevConnect.service;

import com.example.DevConnect.entity.User;
import java.util.List;

import com.example.DevConnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public ResponseEntity<?> createAdmin(User user) {
        try {
            user.setRole(List.of("ADMIN", "USER"));
            user.setPassword(passwordEncoder.encode(user.getPassword()));

            userRepository.save(user);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Admin Created Successfully");
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Something went wrong");
        }
    }
}
