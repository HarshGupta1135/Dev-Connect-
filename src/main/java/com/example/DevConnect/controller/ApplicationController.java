package com.example.DevConnect.controller;

import com.example.DevConnect.entity.Application;
import com.example.DevConnect.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasRole('DEVELOPER')")
    public ResponseEntity<Application> applyToJob(@RequestBody Application application) {
        return ResponseEntity.ok(applicationService.applyToJob(application));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DEVELOPER', 'RECRUITER', 'ADMIN')")
    public ResponseEntity<List<Application>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }
}
