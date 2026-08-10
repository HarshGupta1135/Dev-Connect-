package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.JobPostingRequest;
import com.example.DevConnect.dto.response.JobPostingResponse;
import com.example.DevConnect.dto.response.CustomPageResponse;
import com.example.DevConnect.exception.BadRequestException;
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
import com.example.DevConnect.dto.response.JobPostingWithMatchResponse;
import com.example.DevConnect.entity.DeveloperProfile;
import com.example.DevConnect.repository.DeveloperProfileRepository;
import com.example.DevConnect.util.SkillMatchUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobPostingService {

    @Autowired
    @Lazy
    private JobPostingService self;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private DeveloperProfileRepository developerProfileRepository;

    @Transactional
    @CacheEvict(value = "job-listings", allEntries = true)
    public void createJob(String email, JobPostingRequest jobPostingRequest) {

        // Required-on-create checks live here because the request DTO is shared with the
        // partial-update endpoint, where a missing field means "leave unchanged".
        requireText(jobPostingRequest.getTitle(), "Title is required");
        requireText(jobPostingRequest.getDescription(), "Description is required");
        if (jobPostingRequest.getJobType() == null) {
            throw new BadRequestException("Job type is required");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter Profile Not Found. Please create a profile first."));

        Set<Skill> skillSet = resolveSkills(jobPostingRequest.getRequiredSkills());

        Date expirationDate = jobPostingRequest.getExpiresAt();
        if (expirationDate == null) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 30);
            expirationDate = calendar.getTime();
        }

        // Without a default the job would be saved with status null: invisible in the public
        // listing and impossible to apply to, with no error anywhere.
        JobStatus status = jobPostingRequest.getStatus() != null ? jobPostingRequest.getStatus() : JobStatus.ACTIVE;

        JobPosting jobPosting = JobPosting.builder()
                .recruiter(recruiterProfile)
                .title(jobPostingRequest.getTitle())
                .description(jobPostingRequest.getDescription())
                .jobType(jobPostingRequest.getJobType())
                .location(jobPostingRequest.getLocation())
                .experienceRequired(jobPostingRequest.getExperienceRequired())
                .status(status)
                .requiredSkills(skillSet)
                .expiresAt(expirationDate)
                .build();

        jobPostingRepository.save(jobPosting);
    }

    @Transactional(readOnly = true)
    public CustomPageResponse<JobPostingResponse> getActiveJobs(List<String> skills, String location, JobType jobType,
                                                               Pageable pageable, String developerEmail) {
        List<Long> devSkillIds = new ArrayList<>();
        if (developerEmail != null) {
            User user = userRepository.findByEmail(developerEmail).orElse(null);
            if (user != null) {
                DeveloperProfile devProfile = developerProfileRepository.findByUser(user).orElse(null);
                if (devProfile != null && devProfile.getSkills() != null) {
                    for (Skill skill : devProfile.getSkills()) {
                        if (skill != null && skill.getId() != null) {
                            devSkillIds.add(skill.getId());
                        }
                    }
                }
            }
        }
        // Sort so the same skill set always produces the same cache key.
        Collections.sort(devSkillIds);

        // Normalise the filters that the query treats case-insensitively, so logically
        // identical requests share one cache entry instead of one per spelling.
        List<String> normalisedSkills = normaliseSkills(skills);
        String normalisedLocation = (location != null && !location.isBlank()) ? location.trim().toLowerCase() : null;

        return self.getCachedActiveJobs(normalisedSkills, normalisedLocation, jobType, pageable,
                devSkillIds.isEmpty() ? null : devSkillIds);
    }

    /**
     * The cache key must cover every input that changes the result - including the sort order,
     * otherwise a request for ?sort=title,asc is served the page cached for createdAt,desc.
     */
    @Cacheable(value = "job-listings", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort + '-' + (#skills != null ? #skills : '') + '-' + (#location != null ? #location : '') + '-' + (#jobType != null ? #jobType : '') + '-' + (#devSkillIds != null ? #devSkillIds : '')")
    @Transactional(readOnly = true)
    public CustomPageResponse<JobPostingResponse> getCachedActiveJobs(List<String> skills, String location, JobType jobType,
                                                                     Pageable pageable, List<Long> devSkillIds) {
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
                return cb.lower(skillJoin.get("name")).in(skills);
            });
        }

        if (devSkillIds != null && !devSkillIds.isEmpty()) {
            Set<Long> devSkillIdSet = new HashSet<>(devSkillIds);
            List<JobPosting> allActiveJobs = jobPostingRepository.findAll(spec);
            List<JobPostingWithMatchResponse> matchResponses = new ArrayList<>();

            for (JobPosting job : allActiveJobs) {
                Set<Long> jobSkillIds = new HashSet<>();
                if (job.getRequiredSkills() != null) {
                    for (Skill skill : job.getRequiredSkills()) {
                        if (skill != null && skill.getId() != null) {
                            jobSkillIds.add(skill.getId());
                        }
                    }
                }

                double matchPercentage = SkillMatchUtil.calculateMatchScore(devSkillIdSet, jobSkillIds);
                matchResponses.add(mapToMatchResponse(job, matchPercentage));
            }

            matchResponses.sort(Comparator.comparingDouble(JobPostingWithMatchResponse::getMatchPercentage).reversed());

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), matchResponses.size());

            List<JobPostingResponse> pagedResponses = new ArrayList<>();
            if (start < matchResponses.size()) {
                pagedResponses.addAll(matchResponses.subList(start, end));
            }

            return CustomPageResponse.<JobPostingResponse>builder()
                    .content(pagedResponses)
                    .pageNumber(pageable.getPageNumber())
                    .pageSize(pageable.getPageSize())
                    .totalElements(matchResponses.size())
                    .totalPages((int) Math.ceil((double) matchResponses.size() / pageable.getPageSize()))
                    .last((pageable.getOffset() + pageable.getPageSize()) >= matchResponses.size())
                    .build();
        } else {
            Page<JobPosting> jobPostings = jobPostingRepository.findAll(spec, pageable);
            List<JobPostingResponse> content = new ArrayList<>();
            for (JobPosting job : jobPostings.getContent()) {
                content.add(mapToResponse(job));
            }
            return CustomPageResponse.<JobPostingResponse>builder()
                    .content(content)
                    .pageNumber(jobPostings.getNumber())
                    .pageSize(jobPostings.getSize())
                    .totalElements(jobPostings.getTotalElements())
                    .totalPages(jobPostings.getTotalPages())
                    .last(jobPostings.isLast())
                    .build();
        }
    }

    private JobPostingResponse mapToResponse(JobPosting jobPosting) {
        List<String> skills = jobPosting.getRequiredSkills() != null ?
                jobPosting.getRequiredSkills().stream()
                        .map(Skill::getName)
                        .collect(Collectors.toList()) : List.of();

        String companyName = jobPosting.getRecruiter() != null ? jobPosting.getRecruiter().getCompanyName() : null;

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

    private JobPostingWithMatchResponse mapToMatchResponse(JobPosting jobPosting, double score) {
        List<String> skills = new ArrayList<>();
        if (jobPosting.getRequiredSkills() != null) {
            for (Skill skill : jobPosting.getRequiredSkills()) {
                if (skill != null) {
                    skills.add(skill.getName());
                }
            }
        }

        String companyName = jobPosting.getRecruiter() != null ? jobPosting.getRecruiter().getCompanyName() : null;

        return JobPostingWithMatchResponse.builder()
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
                .matchPercentage(score)
                .build();
    }

    @Transactional(readOnly = true)
    public JobPostingResponse getJobById(Long id) {
        JobPosting jobPosting = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job Posting Not Found with ID: " + id));
        return mapToResponse(jobPosting);
    }

    @Transactional
    @CacheEvict(value = "job-listings", allEntries = true)
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
            requireText(request.getTitle(), "Title cannot be blank");
            jobPosting.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            requireText(request.getDescription(), "Description cannot be blank");
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
            jobPosting.setRequiredSkills(resolveSkills(request.getRequiredSkills()));
        }

        jobPostingRepository.save(jobPosting);
    }

    @Transactional
    // Closing a job removes it from the public listing, so the cached listing must go too -
    // otherwise the closed job keeps being served until the TTL expires.
    @CacheEvict(value = "job-listings", allEntries = true)
    public void closeJobById(String email, Long id) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUser(user).orElseThrow(
                () -> new ResourceNotFoundException("Recruiter Profile Not Found. Please create a profile first"));

        JobPosting jobPosting = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id : " + id));

        if (!jobPosting.getRecruiter().getId().equals(recruiterProfile.getId())) {
            throw new UnauthorizedException("You are not authorized to update this job posting");
        }

        jobPosting.setStatus(JobStatus.CLOSED);
        jobPostingRepository.save(jobPosting);
    }

    @Transactional(readOnly = true)
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

    @CacheEvict(value = "job-listings", allEntries = true)
    public void evictJobListingsCache() {
        // Intentionally empty: Spring Cache handles the eviction
    }

    private Set<Skill> resolveSkills(List<String> skillNames) {
        Set<Skill> skillSet = new HashSet<>();
        if (skillNames == null) {
            return skillSet;
        }
        for (String skillName : skillNames) {
            Skill skill = skillRepository.findByName(skillName)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Skill not found: " + skillName + ". Please contact admin to add it."));
            skillSet.add(skill);
        }
        return skillSet;
    }

    private List<String> normaliseSkills(List<String> skills) {
        if (skills == null) {
            return null;
        }
        List<String> normalised = new ArrayList<>();
        for (String skill : skills) {
            if (skill != null && !skill.isBlank()) {
                normalised.add(skill.trim().toLowerCase());
            }
        }
        if (normalised.isEmpty()) {
            return null;
        }
        Collections.sort(normalised);
        return normalised;
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(message);
        }
    }
}
