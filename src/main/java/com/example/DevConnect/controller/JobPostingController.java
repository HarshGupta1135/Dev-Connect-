package com.example.DevConnect.controller;

import com.example.DevConnect.entity.JobPosting;
import com.example.DevConnect.service.JobPostingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobPostingController {

    @Autowired
    private JobPostingService jobPostingService;

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<JobPosting> createJob(@RequestBody JobPosting jobPosting) {
        return ResponseEntity.ok(jobPostingService.createJob(jobPosting));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JobPosting>> getAllJobs() {
        return ResponseEntity.ok(jobPostingService.getAllJobs());
    }
}
