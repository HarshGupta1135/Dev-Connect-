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
 * Seeds an administrator, but only when one is configured.
 *
 * This used to create `harsh` / `harsh` on any database that did not already have it.
 * That is harmless on a laptop and indefensible anywhere reachable: a fresh deploy would
 * come up with an administrator whose password is in the source history. Now nothing is
 * created unless ADMIN_EMAIL and ADMIN_PASSWORD are both supplied.
 */
@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {

        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.info("No admin credentials configured (app.admin.email / app.admin.password) - skipping admin seed.");
            return;
        }

        if (userRepository.findByUserName(adminUsername) != null
                || userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin user already present - nothing to seed.");
            return;
        }

        User admin = new User();
        admin.setUserName(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(List.of("ADMIN", "USER"));
        userRepository.save(admin);

        // The password is never logged, not even at startup.
        log.info("Seeded admin user '{}' <{}>", adminUsername, adminEmail);
    }
}
