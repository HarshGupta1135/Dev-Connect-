package com.example.DevConnect.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Explicit contract for admin creation. Binding the User entity directly would let a client
 * send an id and turn the create into an overwrite of an existing account.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminCreateRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
            message = "Password must contain at least 1 uppercase letter, 1 digit, 1 special character and be at least 8 characters long"
    )
    private String password;
}
