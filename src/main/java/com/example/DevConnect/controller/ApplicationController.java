package com.example.DevConnect.controller;

import com.example.DevConnect.dto.request.ApplicationRequest;
import com.example.DevConnect.dto.request.StatusUpdateRequest;
import com.example.DevConnect.dto.response.ApiResponse;
import com.example.DevConnect.dto.response.ApplicationResponse;
import com.example.DevConnect.enums.ApplicationStatus;
import com.example.DevConnect.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.file.FileAlreadyExistsException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @PreAuthorize("hasRole('DEVELOPER')")
    @PostMapping("/developer/apply")
    public ResponseEntity<?> applyForJob(@RequestBody ApplicationRequest applicationRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        applicationService.applyForJob(email,applicationRequest);
        return ResponseEntity.ok(ApiResponse.success("Application Submitted Successfully",null));
    }

    @PreAuthorize("hasRole('DEVELOPER')")
    @GetMapping("/developer/applications")
    public ResponseEntity<?> getJobApplications(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        List<ApplicationResponse> data = applicationService.getJobApplications(email);
        return ResponseEntity.ok(ApiResponse.success("Job Applications Fetched Successfully",data));
    }

    @PreAuthorize("hasRole('RECRUITER')")
    @GetMapping("/recruiter/jobs/{jobId}/applications")
    public ResponseEntity<?> getAllApplicantsById(@PathVariable Long jobId){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        List<ApplicationResponse> allApplicantsById = applicationService.getAllApplicantsById(email, jobId);
        return ResponseEntity.ok(ApiResponse.success("Applicants Fetched Successfully",allApplicantsById));
    }

    @PatchMapping("/recruiter/applications/{id}/status")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> setApplicationStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        applicationService.setApplicationStatus(email, id, request.getNewStatus());
        return ResponseEntity.ok(ApiResponse.success("Status Updated Successfully",null));
    }

}
