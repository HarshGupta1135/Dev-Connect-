package com.example.DevConnect.controller;

import com.example.DevConnect.entity.JobPosting;
import com.example.DevConnect.service.JobPostingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.DevConnect.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobPostingController {

    @Autowired
    private JobPostingService jobPostingService;

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> createJob(@RequestBody JobPosting jobPosting) {
        return ResponseEntity.ok(ApiResponse.success("Job created successfully", jobPostingService.createJob(jobPosting)));
    }

    @GetMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> getAllJobs() {
        return ResponseEntity.ok(ApiResponse.success("Jobs fetched successfully", jobPostingService.getAllJobs()));
    }
}
