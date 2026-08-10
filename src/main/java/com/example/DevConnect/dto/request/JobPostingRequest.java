package com.example.DevConnect.dto.request;

import com.example.DevConnect.enums.JobStatus;
import com.example.DevConnect.enums.JobType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * Used for both create and update, so the constraints here only cover format. Fields that are
 * mandatory when creating a job are enforced in JobPostingService#createJob, which keeps
 * partial updates (send only what changes) working.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobPostingRequest {

    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @Size(max = 20000, message = "Description must be at most 20000 characters")
    private String description;

    private JobType jobType;

    @Size(max = 150, message = "Location must be at most 150 characters")
    private String location;

    @Min(value = 0, message = "Experience required cannot be negative")
    @Max(value = 60, message = "Experience required looks unrealistic")
    private Integer experienceRequired;

    private JobStatus status;

    private List<String> requiredSkills;

    @Future(message = "Expiry date must be in the future")
    private Date expiresAt;
}
