package com.example.DevConnect.dto.response;

import com.example.DevConnect.enums.JobStatus;
import com.example.DevConnect.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class JobPostingResponse {
    private Long id;
    private String title;
    private String description;
    private JobType jobType;
    private String location;
    private Integer experienceRequired;
    private JobStatus status;
    private Date createdAt;
    private Date expiresAt;
    private List<String> requiredSkills;
    private String companyName;
}
