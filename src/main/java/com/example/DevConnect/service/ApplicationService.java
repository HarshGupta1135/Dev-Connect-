package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.ApplicationRequest;
import com.example.DevConnect.entity.*;
import com.example.DevConnect.enums.ApplicationStatus;
import com.example.DevConnect.enums.JobStatus;
import com.example.DevConnect.exception.DuplicateApplicationException;
import com.example.DevConnect.exception.ResourceNotFoundException;
import com.example.DevConnect.exception.UnauthorizedException;
import com.example.DevConnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.DevConnect.dto.response.ApplicantSummary;
import com.example.DevConnect.dto.response.ApplicationResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ApplicationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeveloperProfileRepository developerProfileRepository;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;

    @Autowired
    private JobPostingService jobPostingService;

    @Autowired
    private EmailService emailService;

    @Transactional
    public void applyForJob(String email, ApplicationRequest applicationRequest) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        DeveloperProfile developerProfile = developerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Developer Profile Not Found"));

        JobPosting jobPosting = jobPostingRepository.findById(applicationRequest.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job Posting Not Found"));

        if (jobPosting.getStatus() != JobStatus.ACTIVE) {
            throw new RuntimeException("Cannot apply to a closed or inactive job posting");
        }

        if (jobPosting.getExpiresAt() != null && jobPosting.getExpiresAt().before(new Date())) {
            throw new RuntimeException("Cannot apply to an expired job posting");
        }

        if (applicationRepository.existsByDeveloperAndJob(developerProfile, jobPosting)) {
            throw new DuplicateApplicationException("You have already applied for this job posting");
        }

        Application application = Application.builder()
                .developer(developerProfile)
                .job(jobPosting)
                .status(ApplicationStatus.APPLIED)
                .coverNote(applicationRequest.getCoverNote())
                .build();

        applicationRepository.save(application);

        String developerName = developerProfile.getFullName();
        if (developerName == null || developerName.trim().isEmpty()) {
            developerName = user.getUserName();
        }
        if (developerName == null || developerName.trim().isEmpty()) {
            developerName = "Candidate";
        }
        String jobTitle = jobPosting.getTitle();
        String companyName = jobPosting.getRecruiter().getCompanyName();

        // Not the address they signed in with: the one they chose to be notified on.
        emailService.sendApplicationConfirmationEmail(
                user.resolveNotificationEmail(), developerName, jobTitle, companyName);
    }

    public List<ApplicationResponse> getJobApplications(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        DeveloperProfile profile = developerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Developer Profile Not Found"));

        List<Application> list = applicationRepository.findByDeveloper(profile);

        List<ApplicationResponse> responseList = new ArrayList<>();
        for (Application application : list) {
            responseList.add(mapToResponse(application));
        }

        return responseList;

//        return list.stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
    }

    private ApplicationResponse mapToResponse(Application application) {
        String jobTitle = (application.getJob() != null) ? application.getJob().getTitle() : null;
        Long jobId = (application.getJob() != null) ? application.getJob().getId() : null;
        return ApplicationResponse.builder()
                .id(application.getId())
                .jobId(jobId)
                .jobTitle(jobTitle)
                .status(application.getStatus())
                .coverNote(application.getCoverNote())
                .appliedAt(application.getAppliedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }

    /**
     * Runs in a transaction because the applicant summary walks into the developer's
     * lazily loaded skill set, which would otherwise fail once the session is closed.
     */
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getAllApplicantsById(String email, Long jobId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        RecruiterProfile profile = recruiterProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter Profile does not exist"));

        JobPosting jobPosting = jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job Posting Not Found with ID: " + jobId));

        // Verify the job belongs to this recruiter
        if (!jobPosting.getRecruiter().getId().equals(profile.getId())) {
            throw new UnauthorizedException("You are not authorized to view applications for this job posting");
        }

        List<Application> byJob = applicationRepository.findByJob(jobPosting);

        List<ApplicationResponse> list = new ArrayList<>();
        for(Application application : byJob){
            list.add(mapToRecruiterResponse(application));
        }
        return list;
    }

    /**
     * Same shape as the developer's own view, plus the candidate behind the
     * application — the recruiter has to be able to judge experience and skills
     * without leaving the applicants page.
     */
    private ApplicationResponse mapToRecruiterResponse(Application application) {

        ApplicationResponse response = mapToResponse(application);

        DeveloperProfile developer = application.getDeveloper();
        if (developer == null) {
            return response;
        }

        User developerUser = developer.getUser();

        List<String> skillNames = developer.getSkills() != null
                ? developer.getSkills().stream().map(Skill::getName).sorted().toList()
                : List.of();

        response.setApplicant(ApplicantSummary.builder()
                .developerId(developer.getId())
                .fullName(developer.getFullName())
                .userName(developerUser != null ? developerUser.getUserName() : null)
                .email(developerUser != null ? developerUser.getEmail() : null)
                .bio(developer.getBio())
                .location(developer.getLocation())
                .yearsExp(developer.getYearsExp())
                .resumeUrl(developer.getResumeUrl())
                .linkedinUrl(developer.getLinkedinUrl())
                .skills(skillNames)
                .build());

        return response;
    }

    @Transactional
    public void setApplicationStatus(String email, Long id, ApplicationStatus applicationStatus) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter Profile is invalid"));

        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id : " + id));

        // Verify that the job belongs to this recruiter
        if (!application.getJob().getRecruiter().getId().equals(recruiterProfile.getId())) {
            throw new UnauthorizedException("You are not authorized to update this application's status");
        }

        // Validate that new status is either SHORTLISTED or REJECTED
        if (applicationStatus != ApplicationStatus.SHORTLISTED && applicationStatus != ApplicationStatus.REJECTED) {
            throw new RuntimeException("Invalid status update. Status can only be updated to SHORTLISTED or REJECTED");
        }

        application.setStatus(applicationStatus);
        application.setMailSent(true); // Mark as sent to prevent duplicate scheduler runs
        applicationRepository.save(application);

        // Get applicant details
        User applicantUser = application.getDeveloper().getUser();
        String developerName = application.getDeveloper().getFullName();
        if (developerName == null || developerName.trim().isEmpty()) {
            developerName = applicantUser.getUserName();
        }
        if (developerName == null || developerName.trim().isEmpty()) {
            developerName = "Candidate";
        }
        String jobTitle = application.getJob().getTitle();

        // Send status email asynchronously, to the applicant's chosen address
        emailService.sendStatusUpdateEmail(
                applicantUser.resolveNotificationEmail(), developerName, jobTitle, applicationStatus.name());
    }
}
