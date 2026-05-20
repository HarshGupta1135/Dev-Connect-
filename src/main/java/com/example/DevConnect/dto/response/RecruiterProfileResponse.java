package com.example.DevConnect.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecruiterProfileResponse {
    private Long id;
    private String fullName;
    private String companyName;
    private String description;
    private String website;
    private String location;
}
