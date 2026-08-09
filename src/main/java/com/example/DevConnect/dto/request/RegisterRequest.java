package com.example.DevConnect.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Invalid email format"
    )
    private String email;
    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
            message = "Password must contain at least 1 uppercase letter, 1 digit, 1 special character and be at least 8 characters long"
    )
    private String password;
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "DEVELOPER|RECRUITER|developer|recruiter|Developer|Recruiter",

            message = "Role must be either DEVELOPER or RECRUITER")
    private String role;
}
