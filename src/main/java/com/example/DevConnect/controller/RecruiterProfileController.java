package com.example.DevConnect.controller;

import com.example.DevConnect.dto.request.RecruiterProfileRequest;
import com.example.DevConnect.dto.response.ApiResponse;
import com.example.DevConnect.service.RecruiterProfileService;
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
public class RecruiterProfileController {

    @Autowired
    private RecruiterProfileService recruiterProfileService;

    @PostMapping("/recruiter/profile")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> createProfile(@RequestBody RecruiterProfileRequest recruiterProfileRequest){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String message = recruiterProfileService.createProfile(recruiterProfileRequest, email);
        return ResponseEntity.ok(ApiResponse.success(message,null));
    }

}
