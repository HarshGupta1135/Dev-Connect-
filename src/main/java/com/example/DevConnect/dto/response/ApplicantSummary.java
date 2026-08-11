package com.example.DevConnect.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The candidate behind an application, as shown to the recruiter who owns the job.
 *
 * Only populated on the recruiter-facing view: a developer listing their own
 * applications already knows who they are, so it stays null there.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApplicantSummary {
    private Long developerId;
    private String fullName;
    private String userName;
    private String email;
    private String bio;
    private String location;
    private Integer yearsExp;
    private String resumeUrl;
    private String linkedinUrl;
    private List<String> skills;
}
