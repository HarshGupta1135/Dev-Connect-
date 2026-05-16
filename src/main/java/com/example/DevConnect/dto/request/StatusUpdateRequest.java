package com.example.DevConnect.dto.request;

import com.example.DevConnect.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusUpdateRequest {
    private Long applicationId;
    private ApplicationStatus newStatus;
}
