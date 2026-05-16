package com.example.DevConnect.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeveloperProfileResponse {
    private Long id;
    private String fullName;
    private String bio;
    private String location;
    private Integer yearsExp;
    private String resumeUrl;
    private String linkedinUrl;
    private List<String> skills;
}
