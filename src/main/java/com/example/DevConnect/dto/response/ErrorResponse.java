package com.example.DevConnect.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorResponse {

    LocalDateTime timeStamp;
    String errorMessage;
    String errorDetails;
    String errorCode;

    public ErrorResponse(String errorMessage, String errorDetails, String errorCode) {
        this.timeStamp = LocalDateTime.now();
        this.errorMessage = errorMessage;
        this.errorDetails = errorDetails;
        this.errorCode = errorCode;
    }
}
