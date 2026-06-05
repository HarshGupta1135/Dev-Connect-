package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.JobPostingRequest;
import com.example.DevConnect.dto.response.JobPostingResponse;
import com.example.DevConnect.exception.ResourceNotFoundException;
import com.example.DevConnect.exception.UnauthorizedException;
import com.example.DevConnect.entity.JobPosting;
import com.example.DevConnect.entity.RecruiterProfile;
import com.example.DevConnect.entity.Skill;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.enums.JobStatus;
import com.example.DevConnect.enums.JobType;
import com.example.DevConnect.repository.JobPostingRepository;
import com.example.DevConnect.repository.RecruiterProfileRepository;
import com.example.DevConnect.repository.SkillRepository;
import com.example.DevConnect.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobPostingService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Transactional
    public void createJob(String email, JobPostingRequest jobPostingRequest) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User Not Found"));

        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Recruiter Profile Not Found. Please create a profile first."));

        Set<Skill> skillSet = new HashSet<>();
        if (jobPostingRequest.getRequiredSkills() != null) {
            for (String skillName : jobPostingRequest.getRequiredSkills()) {
                Skill skill = skillRepository.findByName(skillName)
                        .orElseThrow(() -> new RuntimeException("Skill not found: " + skillName + ". Please contact admin to add it."));
                skillSet.add(skill);
            }
        }

        Date expirationDate = jobPostingRequest.getExpiresAt();
        if (expirationDate == null) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 30);
            expirationDate = calendar.getTime();
        }

        JobPosting jobPosting = JobPosting.builder()
                .recruiter(recruiterProfile)
                .title(jobPostingRequest.getTitle())
                .description(jobPostingRequest.getDescription())
                .jobType(jobPostingRequest.getJobType())
                .location(jobPostingRequest.getLocation())
                .experienceRequired(jobPostingRequest.getExperienceRequired())
                .status(jobPostingRequest.getStatus())
                .requiredSkills(skillSet)
                .expiresAt(expirationDate)
                .build();

        jobPostingRepository.save(jobPosting);
    }

    public Page<JobPostingResponse> getActiveJobs(List<String> skills, String location, JobType jobType, Pageable pageable) {
        Specification<JobPosting> spec = Specification.where((root, query, cb) -> 
            cb.equal(root.get("status"), JobStatus.ACTIVE)
        );

        if (location != null && !location.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.lower(root.get("location")), "%" + location.trim().toLowerCase() + "%")
            );
        }

        if (jobType != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("jobType"), jobType)
            );
        }

        if (skills != null && !skills.isEmpty()) {
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<JobPosting, Skill> skillJoin = root.join("requiredSkills");
                
                List<String> lowerSkills = skills.stream()
                        .map(String::toLowerCase)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                
                if (lowerSkills.isEmpty()) {
                    return null;
                }
                
                return cb.lower(skillJoin.get("name")).in(lowerSkills);
            });
        }

        Page<JobPosting> jobPostings = jobPostingRepository.findAll(spec, pageable);
        return jobPostings.map(this::mapToResponse);
    }

    private JobPostingResponse mapToResponse(JobPosting jobPosting) {
        List<String> skills = jobPosting.getRequiredSkills() != null ?
                jobPosting.getRequiredSkills().stream()
                        .map(Skill::getName)
                        .collect(Collectors.toList()) : List.of();

        String companyName = jobPosting.getRecruiter() != null && jobPosting.getRecruiter().getCompanyName() != null ?
                jobPosting.getRecruiter().getCompanyName() : null;

        return JobPostingResponse.builder()
                .id(jobPosting.getId())
                .title(jobPosting.getTitle())
                .description(jobPosting.getDescription())
                .jobType(jobPosting.getJobType())
                .location(jobPosting.getLocation())
                .experienceRequired(jobPosting.getExperienceRequired())
                .status(jobPosting.getStatus())
                .createdAt(jobPosting.getCreatedAt())
                .expiresAt(jobPosting.getExpiresAt())
                .requiredSkills(skills)
                .companyName(companyName)
                .build();
    }

    public JobPostingResponse getJobById(Long id) {
        JobPosting jobPosting = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job Posting Not Found with ID: " + id));
        return mapToResponse(jobPosting);
    }

    @Transactional
    public void updateJob(String email, Long id, JobPostingRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter Profile Not Found. Please create a profile first."));

        JobPosting jobPosting = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job Posting Not Found with ID: " + id));

        // CRUCIAL SECURITY CHECK: Verify the job belongs to the logged-in recruiter
        if (!jobPosting.getRecruiter().getId().equals(recruiterProfile.getId())) {
            throw new UnauthorizedException("You are not authorized to update this job posting");
        }

        // Update fields if provided in request
        if (request.getTitle() != null) {
            jobPosting.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            jobPosting.setDescription(request.getDescription());
        }
        if (request.getJobType() != null) {
            jobPosting.setJobType(request.getJobType());
        }
        if (request.getLocation() != null) {
            jobPosting.setLocation(request.getLocation());
        }
        if (request.getExperienceRequired() != null) {
            jobPosting.setExperienceRequired(request.getExperienceRequired());
        }
        if (request.getStatus() != null) {
            jobPosting.setStatus(request.getStatus());
        }
        if (request.getExpiresAt() != null) {
            jobPosting.setExpiresAt(request.getExpiresAt());
        }

        // Update required skills if provided
        if (request.getRequiredSkills() != null) {
            Set<Skill> skillSet = new HashSet<>();
            for (String skillName : request.getRequiredSkills()) {
                Skill skill = skillRepository.findByName(skillName)
                        .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + skillName + ". Please contact admin to add it."));
                skillSet.add(skill);
            }
            jobPosting.setRequiredSkills(skillSet);
        }

        jobPostingRepository.save(jobPosting);
    }

    @Transactional
    public void closeJobById(String email, Long id) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUser(user).orElseThrow(
                () -> new ResourceNotFoundException("Recruiter Profile Not Found. Please create a profile first"));

        JobPosting jobPosting = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id : " + id));

        if (!jobPosting.getRecruiter().getId().equals(recruiterProfile.getId())) {
            throw new UnauthorizedException("You are not authorized to update this job posting");
        }

        jobPosting.setStatus(JobStatus.CLOSED);

    }

    public List<JobPostingResponse> getAllJobsByRecruiter(String email) {

        User user = userRepository.findByEmail(email).
                orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter Profile Not Found. Please create a profile first"));

        List<JobPosting> jobs = jobPostingRepository.findByRecruiter(recruiterProfile);
        List<JobPostingResponse> responseList = new ArrayList<>();
        for (JobPosting job : jobs) {
            responseList.add(mapToResponse(job));
        }
        return responseList;

    }
}
