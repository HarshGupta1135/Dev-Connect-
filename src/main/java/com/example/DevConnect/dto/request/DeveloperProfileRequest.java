package com.example.DevConnect.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeveloperProfileRequest {
    private String fullName;
    private String bio;
    private String location;
    private List<String> skills;
    private String linkedinUrl;
}
