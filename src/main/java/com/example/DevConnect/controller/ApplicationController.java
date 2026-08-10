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

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Job Applications", description = "Endpoints for managing job applications")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @PreAuthorize("hasRole('DEVELOPER')")
    @PostMapping("/developer/apply")
    @Operation(summary = "Apply for a job listing", description = "Allows an authenticated developer to submit a job application.")
    public ResponseEntity<?> applyForJob(@RequestBody ApplicationRequest applicationRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        applicationService.applyForJob(email,applicationRequest);
        return ResponseEntity.ok(ApiResponse.success("Application Submitted Successfully",null));
    }

    @PreAuthorize("hasRole('DEVELOPER')")
    @GetMapping("/developer/applications")
    @Operation(summary = "Get developer job applications", description = "Retrieves all job applications submitted by the logged-in developer.")
    public ResponseEntity<?> getJobApplications(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        List<ApplicationResponse> data = applicationService.getJobApplications(email);
        return ResponseEntity.ok(ApiResponse.success("Job Applications Fetched Successfully",data));
    }

    @PreAuthorize("hasRole('RECRUITER')")
    @GetMapping("/recruiter/jobs/{jobId}/applications")
    @Operation(summary = "Get all applications for a specific job", description = "Retrieves all applicants and status info for a specific job posting. Validates recruiter ownership first.")
    public ResponseEntity<?> getAllApplicantsById(@PathVariable Long jobId){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        List<ApplicationResponse> allApplicantsById = applicationService.getAllApplicantsById(email, jobId);
        return ResponseEntity.ok(ApiResponse.success("Applicants Fetched Successfully",allApplicantsById));
    }

    @PatchMapping("/recruiter/applications/{id}/status")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Update job application status", description = "Updates status of a job application (e.g. SHORTLISTED, REJECTED). Validates recruiter ownership first.")
    public ResponseEntity<?> setApplicationStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        applicationService.setApplicationStatus(email, id, request.getNewStatus());
        return ResponseEntity.ok(ApiResponse.success("Status Updated Successfully",null));
    }

}
