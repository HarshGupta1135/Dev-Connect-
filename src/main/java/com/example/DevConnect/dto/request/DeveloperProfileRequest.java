package com.example.DevConnect.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Shared by create and update; required-on-create checks live in DeveloperProfileService.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeveloperProfileRequest {

    @Size(max = 120, message = "Full name must be at most 120 characters")
    private String fullName;

    @Size(max = 5000, message = "Bio must be at most 5000 characters")
    private String bio;

    @Size(max = 150, message = "Location must be at most 150 characters")
    private String location;

    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 60, message = "Years of experience looks unrealistic")
    private Integer yearsExp;

    private List<String> skills;

    @Size(max = 300, message = "LinkedIn URL must be at most 300 characters")
    private String linkedinUrl;
}
