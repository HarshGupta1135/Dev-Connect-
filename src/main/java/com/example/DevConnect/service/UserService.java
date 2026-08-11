package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.AccountUpdateRequest;
import com.example.DevConnect.dto.response.AccountResponse;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.exception.ResourceNotFoundException;
import com.example.DevConnect.repository.UserRepository;
import com.example.DevConnect.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The signed-in user's own login details. Both roles share this: username and
 * email live on the user record, not on the developer or recruiter profile.
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

        String newUserName = request.getUserName() != null ? request.getUserName().trim() : null;
        String newEmail = request.getEmail() != null ? request.getEmail().trim() : null;

        if (newUserName != null && !newUserName.isBlank() && !newUserName.equals(user.getUserName())) {
            User existing = userRepository.findByUserName(newUserName);
            if (existing != null && !existing.getId().equals(user.getId())) {
                throw new RuntimeException("That username is already taken");
            }
            user.setUserName(newUserName);
        }

        boolean emailChanged = false;
        if (newEmail != null && !newEmail.isBlank() && !newEmail.equals(user.getEmail())) {

            // Registration only accepts Gmail addresses, so editing has to hold the same
            // line — otherwise an account could end up on an address the mailer refuses.
            if (!newEmail.toLowerCase().endsWith("@gmail.com")) {
                throw new RuntimeException("Email must be a @gmail.com address");
            }

            boolean taken = userRepository.findByEmail(newEmail)
                    .filter(other -> !other.getId().equals(user.getId()))
                    .isPresent();
            if (taken) {
                throw new RuntimeException("That email is already registered to another account");
            }

            user.setEmail(newEmail);
            emailChanged = true;
        }

        User saved = userRepository.save(user);

        // See AccountResponse#token: the old JWT names the previous email as its
        // subject, so a changed address needs a freshly signed token to go with it.
        String token = emailChanged ? jwtUtil.generateToken(new UserPrincipal(saved)) : null;

        return toResponse(saved, token);
    }

    private AccountResponse toResponse(User user, String token) {
        return AccountResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreated_at())
                .token(token)
                .build();
    }
}
