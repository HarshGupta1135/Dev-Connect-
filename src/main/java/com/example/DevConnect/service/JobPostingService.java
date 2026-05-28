package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.JobPostingRequest;
import com.example.DevConnect.entity.JobPosting;
import com.example.DevConnect.entity.RecruiterProfile;
import com.example.DevConnect.entity.Skill;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.repository.JobPostingRepository;
import com.example.DevConnect.repository.RecruiterProfileRepository;
import com.example.DevConnect.repository.SkillRepository;
import com.example.DevConnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
}
