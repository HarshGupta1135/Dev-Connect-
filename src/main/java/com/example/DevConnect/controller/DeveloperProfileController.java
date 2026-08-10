package com.example.DevConnect.controller;

import com.example.DevConnect.dto.request.DeveloperProfileRequest;
import com.example.DevConnect.dto.response.ApiResponse;
import com.example.DevConnect.dto.response.DeveloperProfileResponse;
import com.example.DevConnect.service.DeveloperProfileService;
import com.example.DevConnect.service.CloudinaryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Developer Profile", description = "Endpoints for managing developer profiles and resumes")
public class DeveloperProfileController {

    @Autowired
    private DeveloperProfileService developerService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @PreAuthorize("hasRole('DEVELOPER')")
    @PostMapping("/developer/profile")
    @Operation(summary = "Create developer profile", description = "Creates a new developer profile for the authenticated developer.")
    public ResponseEntity<?> createProfile(@Valid @RequestBody DeveloperProfileRequest developerProfileRequest){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String message = developerService.createProfile(developerProfileRequest,email);
        return ResponseEntity.ok(ApiResponse.success(message,null));
    }

    @PutMapping("/developer/profile")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "Update developer profile", description = "Updates profile details for the authenticated developer.")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody DeveloperProfileRequest developerProfileRequest){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String message = developerService.updateProfile(developerProfileRequest,email);
        return ResponseEntity.ok(ApiResponse.success(message,null));
    }

    @GetMapping("/developer/profile/me")
    @PreAuthorize("hasRole('DEVELOPER')")
    @Operation(summary = "Get current developer profile", description = "Retrieves the profile of the logged-in developer.")
    public ResponseEntity<?> getProfile(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        DeveloperProfileResponse data = developerService.getProfile(email);
        return ResponseEntity.ok(ApiResponse.success("Profile Fetched Successfully",data));
    }

    @PreAuthorize("hasRole('DEVELOPER')")
    @PostMapping(value = "/developer/profile/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload developer resume", description = "Uploads a resume file to Cloudinary and links it to the developer's profile.")
    public ResponseEntity<?> uploadResume(@RequestParam("file") MultipartFile file){
        // File validation (empty, content type) and error mapping live in the service and the
        // global handler, so a Cloudinary outage is no longer reported to the client as a 400.
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String resumeUrl = cloudinaryService.uploadResume(file);
        developerService.updateResumeUrl(email, resumeUrl);
        return ResponseEntity.ok(ApiResponse.success("Resume uploaded successfully!", Map.of("resumeUrl", resumeUrl)));
    }
}
