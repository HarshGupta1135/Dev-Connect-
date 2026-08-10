package com.example.DevConnect.controller;

import com.example.DevConnect.dto.request.RecruiterProfileRequest;
import com.example.DevConnect.dto.response.ApiResponse;
import com.example.DevConnect.dto.response.RecruiterProfileResponse;
import com.example.DevConnect.dto.response.DeveloperProfileResponse;
import com.example.DevConnect.service.RecruiterProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Recruiter Profile", description = "Endpoints for managing recruiter profiles and viewing candidate profiles")
public class RecruiterProfileController {

    @Autowired
    private RecruiterProfileService recruiterProfileService;

    @PostMapping("/recruiter/profile")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Create recruiter profile", description = "Creates a new recruiter profile for the authenticated user.")
    public ResponseEntity<?> createProfile(@Valid @RequestBody RecruiterProfileRequest recruiterProfileRequest){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String message = recruiterProfileService.createProfile(recruiterProfileRequest, email);
        return ResponseEntity.ok(ApiResponse.success(message,null));
    }

    @GetMapping("/recruiter/profile/me")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Get current recruiter profile", description = "Retrieves the profile of the logged-in recruiter.")
    public ResponseEntity<?> getProfile(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        RecruiterProfileResponse profile = recruiterProfileService.getProfile(email);
        return ResponseEntity.ok(ApiResponse.success("Profile Fetched Successfully",profile));
    }

    @PreAuthorize("hasRole('RECRUITER')")
    @PutMapping("/recruiter/profile")
    @Operation(summary = "Update recruiter profile", description = "Updates details of the recruiter's profile.")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody RecruiterProfileRequest recruiterProfileRequest){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        recruiterProfileService.updateProfile(recruiterProfileRequest,email);
        return ResponseEntity.ok(ApiResponse.success("Profile Updated Successfully",null));
    }

    @GetMapping("/developer/profile/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Get developer profile by ID", description = "Allows an authenticated recruiter to view a specific developer's profile and resume.")
    public ResponseEntity<?> getDeveloperProfile(@PathVariable Long id){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        DeveloperProfileResponse developerProfile = recruiterProfileService.getDeveloperProfile(email, id);
        return ResponseEntity.ok(ApiResponse.success("Profile Retrieved Successfully",developerProfile));
    }

}
