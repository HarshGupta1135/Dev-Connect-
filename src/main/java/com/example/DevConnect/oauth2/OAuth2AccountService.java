package com.example.DevConnect.oauth2;

import com.example.DevConnect.entity.User;
import com.example.DevConnect.repository.UserRepository;
import com.example.DevConnect.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Turns a verified provider email into a DevConnect user.
 *
 * Accounts are matched on email, which is also the JWT subject everywhere else. So a
 * user who registered with a password and later clicks "Continue with Google" for
 * the same address lands in the same account — safe only because the caller passes
 * an address the provider has verified.
 */
@Service
public class OAuth2AccountService {

    private static final int MAX_USERNAME_LENGTH = 30;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    /** A sign-in that cannot be completed; the code is shown to the frontend. */
    public static class OAuth2SignInException extends RuntimeException {
        private final String code;

        public OAuth2SignInException(String code) {
            super(code);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    /**
     * @param verifiedEmail address the provider has verified
     * @param suggestedName the provider's display name or login, used for a new username
     * @param signupRole    DEVELOPER or RECRUITER from the register page, or null when
     *                      the user started from the login page
     */
    @Transactional
    public User findOrCreate(String verifiedEmail, String suggestedName, String signupRole) {
        String email = verifiedEmail.trim();

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            return existing.get();
        }

        // No account yet, and nothing says which kind to create. The frontend sends the
        // user to the register page to pick a role and try again.
        if (signupRole == null) {
            throw new OAuth2SignInException("role_required");
        }

        User user = new User();
        user.setUserName(uniqueUserName(suggestedName, email));
        user.setEmail(email);
        // No password: this account signs in through the provider. The password login
        // rejects it, since a null hash never matches.
        user.setPassword(null);
        user.setRole(List.of(signupRole));

        userRepository.save(user);
        emailService.sendWelcomeEmail(user.resolveNotificationEmail(), user.getUserName());
        return user;
    }

    private String uniqueUserName(String suggestedName, String email) {
        String base = sanitize(suggestedName);
        if (base.isEmpty()) {
            base = sanitize(email.substring(0, email.indexOf('@') > 0 ? email.indexOf('@') : email.length()));
        }
        if (base.isEmpty()) {
            base = "user";
        }

        String candidate = base;
        for (int suffix = 1; userRepository.findByUserName(candidate) != null; suffix++) {
            String tail = String.valueOf(suffix);
            candidate = base.substring(0, Math.min(base.length(), MAX_USERNAME_LENGTH - tail.length())) + tail;
        }
        return candidate;
    }

    private static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.trim().replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9_.-]", "");
        return cleaned.length() > MAX_USERNAME_LENGTH ? cleaned.substring(0, MAX_USERNAME_LENGTH) : cleaned;
    }
}
