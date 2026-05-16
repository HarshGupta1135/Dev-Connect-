package com.example.DevConnect.dto.request;

import com.example.DevConnect.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobPostingRequest {
    private String title;
    private String description;
    private JobType jobType;
    private String location;
    private Integer experienceRequired;
    private List<String> requiredSkills;
}
