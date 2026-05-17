package com.example.DevConnect.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeveloperProfileRequest {
    @NotBlank(message = "Full username is required")
    private String fullName;
    private String bio;
    @NotBlank(message = "Please put your location")
    private String location;
    private Integer yearsExp;
    private List<String> skills;
    private String linkedinUrl;
}
