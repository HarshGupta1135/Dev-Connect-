package com.example.DevConnect.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecruiterProfileRequest {
    private String companyName;
    private String description;
    private String website;
    private String location;
}
