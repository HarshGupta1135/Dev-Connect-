package com.example.DevConnect.controller;

import com.example.DevConnect.dto.request.JobPostingRequest;
import com.example.DevConnect.dto.response.ApiResponse;
import com.example.DevConnect.service.JobPostingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class JobPostingController {

    @Autowired
    private JobPostingService jobPostingService;

    @PostMapping("/recruiter/jobs")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> createJob(@RequestBody JobPostingRequest jobPostingRequest){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        jobPostingService.createJob(email,jobPostingRequest);
        return ResponseEntity.ok(ApiResponse.success("Job Created Successfully",null));
    }

}
