package com.example.DevConnect.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationRequest {

    @NotNull(message = "jobId is required")
    private Long jobId;

    @Size(max = 5000, message = "Cover note must be at most 5000 characters")
    private String coverNote;
}
