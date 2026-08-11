package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.AccountUpdateRequest;
import com.example.DevConnect.dto.response.AccountResponse;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.enums.EmailPreference;
import com.example.DevConnect.exception.BadRequestException;
import com.example.DevConnect.exception.ConflictException;
import com.example.DevConnect.exception.ResourceNotFoundException;
import com.example.DevConnect.repository.UserRepository;
import com.example.DevConnect.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The signed-in user's own login details. Both roles share this: username, email
 * and notification preference live on the user record, not on the developer or
 * recruiter profile.
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public AccountResponse getAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));
        return toResponse(user, null);
    }

    @Transactional
    public AccountResponse updateAccount(String email, AccountUpdateRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        applyUserName(user, request.getUserName());
        boolean emailChanged = applyPrimaryEmail(user, request.getEmail());
        applySecondaryEmail(user, request.getSecondaryEmail());
        applyEmailPreference(user, request.getEmailPreference());

        User saved = userRepository.save(user);

        // See AccountResponse#token: the old JWT names the previous email as its
        // subject, so a changed address needs a freshly signed token to go with it.
        String token = emailChanged ? jwtUtil.generateToken(new UserPrincipal(saved)) : null;

        return toResponse(saved, token);
    }

    private void applyUserName(User user, String requested) {
        String newUserName = requested != null ? requested.trim() : null;
        if (newUserName == null || newUserName.isBlank() || newUserName.equals(user.getUserName())) {
            return;
        }

        User existing = userRepository.findByUserName(newUserName);
        if (existing != null && !existing.getId().equals(user.getId())) {
            throw new ConflictException("That username is already taken");
        }
        user.setUserName(newUserName);
    }

    /** Returns true when the sign-in address actually changed. */
    private boolean applyPrimaryEmail(User user, String requested) {
        String newEmail = requested != null ? requested.trim() : null;
        if (newEmail == null || newEmail.isBlank() || newEmail.equals(user.getEmail())) {
            return false;
        }

        // Registration only accepts Gmail addresses, so editing has to hold the same
        // line — otherwise an account could end up on an address the mailer refuses.
        if (!newEmail.toLowerCase().endsWith("@gmail.com")) {
            throw new BadRequestException("Email must be a @gmail.com address");
        }

        boolean taken = userRepository.findByEmail(newEmail)
                .filter(other -> !other.getId().equals(user.getId()))
                .isPresent();
        if (taken) {
            throw new ConflictException("That email is already registered to another account");
        }

        user.setEmail(newEmail);
        return true;
    }

    private void applySecondaryEmail(User user, String requested) {
        if (requested == null) {
            return;
        }

        String secondary = requested.trim();

        // Blank clears it. A SECONDARY preference cannot survive that, or notifications
        // would silently fall back with the UI still claiming they go elsewhere.
        if (secondary.isBlank()) {
            user.setSecondaryEmail(null);
            if (user.getEmailPreference() == EmailPreference.SECONDARY) {
                user.setEmailPreference(EmailPreference.PRIMARY);
            }
            return;
        }

        if (secondary.equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException("Your secondary email must be different from your primary email");
        }

        user.setSecondaryEmail(secondary);
    }

    private void applyEmailPreference(User user, EmailPreference requested) {
        if (requested == null) {
            return;
        }

        if (requested == EmailPreference.SECONDARY
                && (user.getSecondaryEmail() == null || user.getSecondaryEmail().isBlank())) {
            throw new BadRequestException("Add a secondary email before choosing to receive mail there");
        }

        user.setEmailPreference(requested);
    }

    private AccountResponse toResponse(User user, String token) {
        return AccountResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .secondaryEmail(user.getSecondaryEmail())
                // Null on rows that predate the column; the client should see the
                // effective value, not the absence of one.
                .emailPreference(user.getEmailPreference() != null
                        ? user.getEmailPreference()
                        : EmailPreference.PRIMARY)
                .notificationEmail(user.resolveNotificationEmail())
                .role(user.getRole())
                .createdAt(user.getCreated_at())
                .token(token)
                .build();
    }
}
