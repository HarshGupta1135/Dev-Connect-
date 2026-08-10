package com.example.DevConnect.config;

import com.example.DevConnect.entity.User;
import com.example.DevConnect.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Optional bootstrap of the first admin account. Disabled by default so that no
 * environment ever gets a well-known admin credential just by starting the app;
 * enable it explicitly with app.admin.bootstrap.enabled and supply the credentials.
 */
@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.bootstrap.enabled:false}")
    private boolean bootstrapEnabled;

    @Value("${app.admin.bootstrap.username:}")
    private String adminUsername;

    @Value("${app.admin.bootstrap.email:}")
    private String adminEmail;

    @Value("${app.admin.bootstrap.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!bootstrapEnabled) {
            return;
        }

        if (adminUsername.isBlank() || adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("Admin bootstrap is enabled but username/email/password are not fully configured. Skipping.");
            return;
        }

        if (userRepository.findByUserName(adminUsername) != null) {
            log.info("Admin bootstrap skipped: user '{}' already exists.", adminUsername);
            return;
        }

        User admin = new User();
        admin.setUserName(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setEmail(adminEmail);
        admin.setRole(List.of("ADMIN", "USER"));
        userRepository.save(admin);

        log.info("Bootstrap admin user '{}' created.", adminUsername);
    }
}
