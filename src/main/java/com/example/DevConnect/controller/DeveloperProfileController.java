package com.example.DevConnect.controller;

import com.example.DevConnect.dto.request.DeveloperProfileRequest;
import com.example.DevConnect.dto.response.ApiResponse;
import com.example.DevConnect.dto.response.DeveloperProfileResponse;
import com.example.DevConnect.service.DeveloperProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DeveloperProfileController {

    @Autowired
    private DeveloperProfileService developerService;

    @PreAuthorize("hasRole('DEVELOPER')")
    @PostMapping("/developer/profile")
    public ResponseEntity<?> createProfile(@RequestBody DeveloperProfileRequest developerProfileRequest){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String message = developerService.createProfile(developerProfileRequest,email);
        return ResponseEntity.ok(ApiResponse.success(message,null));
    }

    @PutMapping("/developer/profile")
    @PreAuthorize("hasRole('DEVELOPER')")
    public ResponseEntity<?> updateProfile(@RequestBody DeveloperProfileRequest developerProfileRequest){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String message = developerService.updateProfile(developerProfileRequest,email);
        return ResponseEntity.ok(ApiResponse.success(message,null));
    }

    @GetMapping("/developer/profile/me")
    @PreAuthorize("hasRole('DEVELOPER')")
    public ResponseEntity<?> getProfile(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        DeveloperProfileResponse data = developerService.getProfile(email);
        return ResponseEntity.ok(ApiResponse.success("Profile Fetched Successfully",data));
    }

}
