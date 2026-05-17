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
    private String email;
    private String password;
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "DEVELOPER|RECRUITER|developer|recruiter|Developer|Recruiter",

            message = "Role must be either DEVELOPER or RECRUITER")
    private String role;
}
