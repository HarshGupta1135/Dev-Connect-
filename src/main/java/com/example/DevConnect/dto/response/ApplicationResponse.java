package com.example.DevConnect.dto.response;

import com.example.DevConnect.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApplicationResponse {
    private Long id;

    /**
     * The posting applied to. Without it a client can only match applications by
     * title, which conflates a reposted role with the original — and then treats a
     * decision on one posting as if it applied to the other.
     */
    private Long jobId;

    private String jobTitle;
    private ApplicationStatus status;
    private String coverNote;
    private Date appliedAt;
    private Date updatedAt;

    /** Set on the recruiter's applicants view only; null when a developer lists their own. */
    private ApplicantSummary applicant;
}
