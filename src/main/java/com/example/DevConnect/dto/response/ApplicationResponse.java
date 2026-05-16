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
    private String jobTitle;
    private ApplicationStatus status;
    private String coverNote;
    private Date appliedAt;
    private Date updatedAt;
}
