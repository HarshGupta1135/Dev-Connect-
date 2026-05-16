package com.example.DevConnect.config;

import com.example.DevConnect.entity.User;
import com.example.DevConnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Check if an admin already exists to avoid duplicate creation
        if (userRepository.findByUserName("harsh") == null) {
            User admin = new User();
            admin.setUserName("harsh");
            admin.setPassword(passwordEncoder.encode("harsh"));
            admin.setEmail("harsh@gmail.com");
            admin.setRole(List.of("ADMIN", "USER"));
            userRepository.save(admin);
            System.out.println("Default Admin User Created: harsh / harsh");
        }
    }
}
