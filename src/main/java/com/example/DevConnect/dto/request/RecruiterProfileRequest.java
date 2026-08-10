package com.example.DevConnect.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shared by create and update; required-on-create checks live in RecruiterProfileService.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecruiterProfileRequest {

    @Size(max = 120, message = "Full name must be at most 120 characters")
    private String fullName;

    @Size(max = 150, message = "Company name must be at most 150 characters")
    private String companyName;

    @Size(max = 5000, message = "Description must be at most 5000 characters")
    private String description;

    @Size(max = 300, message = "Website must be at most 300 characters")
    private String website;

    @Size(max = 150, message = "Location must be at most 150 characters")
    private String location;
}
