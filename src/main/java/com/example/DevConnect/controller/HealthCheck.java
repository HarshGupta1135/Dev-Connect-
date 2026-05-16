package com.example.DevConnect.controller;

import com.example.DevConnect.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthCheck {

    @GetMapping
    public ResponseEntity<?> healthCheck(){
        return ResponseEntity.ok(ApiResponse.success("Health check passed", "hello"));
    }

}
