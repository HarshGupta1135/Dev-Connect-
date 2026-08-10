package com.example.DevConnect.dto.request;

import com.example.DevConnect.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusUpdateRequest {

    @NotNull(message = "newStatus is required and must be SHORTLISTED or REJECTED")
    private ApplicationStatus newStatus;
}
