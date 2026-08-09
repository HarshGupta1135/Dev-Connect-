package com.example.DevConnect.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecruiterProfileRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;
    private String companyName;
    private String description;
    private String website;
    private String location;
}
