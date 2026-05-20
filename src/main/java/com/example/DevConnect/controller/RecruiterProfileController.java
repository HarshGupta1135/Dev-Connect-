package com.example.DevConnect.controller;

import com.example.DevConnect.dto.request.RecruiterProfileRequest;
import com.example.DevConnect.dto.response.ApiResponse;
import com.example.DevConnect.dto.response.RecruiterProfileResponse;
import com.example.DevConnect.service.RecruiterProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RecruiterProfileController {

    @Autowired
    private RecruiterProfileService recruiterProfileService;

    @PostMapping("/recruiter/profile")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> createProfile(@Valid @RequestBody RecruiterProfileRequest recruiterProfileRequest){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String message = recruiterProfileService.createProfile(recruiterProfileRequest, email);
        return ResponseEntity.ok(ApiResponse.success(message,null));
    }

    @GetMapping("/recruiter/profile/me")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> getProfile(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        RecruiterProfileResponse profile = recruiterProfileService.getProfile(email);
        return ResponseEntity.ok(ApiResponse.success("Profile Fetched Successfully",profile));
    }

    @PreAuthorize("hasRole('RECRUITER')")
    @PutMapping("/recruiter/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody RecruiterProfileRequest recruiterProfileRequest){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        recruiterProfileService.updateProfile(recruiterProfileRequest,email);
        return ResponseEntity.ok(ApiResponse.success("Profile Updated Successfully",null));
    }

}
