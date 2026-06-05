package com.example.DevConnect.controller;

import com.example.DevConnect.dto.request.JobPostingRequest;
import com.example.DevConnect.dto.response.ApiResponse;
import com.example.DevConnect.dto.response.JobPostingResponse;
import com.example.DevConnect.enums.JobType;
import com.example.DevConnect.service.JobPostingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


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

    @GetMapping("/jobs")
    public ResponseEntity<?> getActiveJobs(
            @RequestParam(required = false) List<String> skills,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) JobType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        List<Sort.Order> orders = new ArrayList<>();
        if (sort != null) {
            if (sort.length == 2 && (sort[1].equalsIgnoreCase("asc") || sort[1].equalsIgnoreCase("desc"))) {
                Sort.Direction direction = sort[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
                orders.add(new Sort.Order(direction, sort[0].trim()));
            } else {
                for (String sortPart : sort) {
                    String[] parts = sortPart.split(",");
                    String property = parts[0].trim();
                    Sort.Direction direction = Sort.Direction.DESC; // default
                    if (parts.length > 1) {
                        if (parts[1].trim().equalsIgnoreCase("asc")) {
                            direction = Sort.Direction.ASC;
                        }
                    }
                    orders.add(new Sort.Order(direction, property));
                }
            }
        }
        Sort sortOrder = orders.isEmpty() ? Sort.by(Sort.Direction.DESC, "createdAt") : Sort.by(orders);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<JobPostingResponse> activeJobs = jobPostingService.getActiveJobs(skills, location, type, pageable);
        return ResponseEntity.ok(ApiResponse.success("Active jobs fetched successfully", activeJobs));
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<?> getJobById(@PathVariable Long id){
        JobPostingResponse job = jobPostingService.getJobById(id);
        return ResponseEntity.ok(ApiResponse.success("Job Fetched Successfully", job));
    }

    @PutMapping("/recruiter/jobs/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> updateJob(@PathVariable Long id, @RequestBody JobPostingRequest jobPostingRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        jobPostingService.updateJob(email, id, jobPostingRequest);
        return ResponseEntity.ok(ApiResponse.success("Job Updated Successfully", null));
    }

    @PatchMapping("/recruiter/jobs/{id}/close")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> closeJobById(@PathVariable Long id){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        jobPostingService.closeJobById(email,id);
        return ResponseEntity.ok(ApiResponse.success("Job Closed Successfully",null));
    }

    @GetMapping("/recruiter/jobs")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<?> getAllJobsByRecruiter(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        List<JobPostingResponse> jobs = jobPostingService.getAllJobsByRecruiter(email);
        return ResponseEntity.ok(ApiResponse.success("Jobs Fetched Successfully", jobs));
    }
}
