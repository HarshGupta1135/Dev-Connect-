package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.RecruiterProfileRequest;
import com.example.DevConnect.dto.response.RecruiterProfileResponse;
import com.example.DevConnect.dto.response.DeveloperProfileResponse;
import com.example.DevConnect.entity.DeveloperProfile;
import com.example.DevConnect.entity.RecruiterProfile;
import com.example.DevConnect.entity.Skill;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.repository.DeveloperProfileRepository;
import com.example.DevConnect.repository.RecruiterProfileRepository;
import com.example.DevConnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecruiterProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;

    @Autowired
    private DeveloperProfileRepository developerProfileRepository;

    @Transactional
    public String createProfile(RecruiterProfileRequest recruiterProfileRequest, String email) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User Not Found"));

        if(recruiterProfileRepository.findByUser(user).isPresent()){
            throw new RuntimeException("Profile already exists for this user");
        }

        RecruiterProfile recruiterProfile = RecruiterProfile.builder()
                .user(user)
                .fullName(recruiterProfileRequest.getFullName())
                .companyName(recruiterProfileRequest.getCompanyName())
                .companyDesc(recruiterProfileRequest.getDescription())
                .website(recruiterProfileRequest.getWebsite())
                .location(recruiterProfileRequest.getLocation())
                .build();

        recruiterProfileRepository.save(recruiterProfile);

        return "Profile Created Successfully";

    }

    public RecruiterProfileResponse getProfile(String email) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User Not Found"));

        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Profile Not Found"));

        return RecruiterProfileResponse.builder()
                .id(recruiterProfile.getId())
                .fullName(recruiterProfile.getFullName())
                .companyName(recruiterProfile.getCompanyName())
                .description(recruiterProfile.getCompanyDesc())
                .website(recruiterProfile.getWebsite())
                .location(recruiterProfile.getLocation())
                .build();

    }

    @Transactional
    public void updateProfile(RecruiterProfileRequest recruiterProfileRequest, String email) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User Not Found"));

        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Profile does not exist for this user"));

        if(recruiterProfileRequest.getFullName() != null && !recruiterProfileRequest.getFullName().isBlank()){
            recruiterProfile.setFullName(recruiterProfileRequest.getFullName());
        }

        if(recruiterProfileRequest.getCompanyName() != null && !recruiterProfileRequest.getCompanyName().isBlank()){
            recruiterProfile.setCompanyName(recruiterProfileRequest.getCompanyName());
        }

        if(recruiterProfileRequest.getDescription() != null && !recruiterProfileRequest.getDescription().isBlank()){
            recruiterProfile.setCompanyDesc(recruiterProfileRequest.getDescription());
        }

        if(recruiterProfileRequest.getLocation() != null && !recruiterProfileRequest.getLocation().isBlank()){
            recruiterProfile.setLocation(recruiterProfileRequest.getLocation());
        }

        if(recruiterProfileRequest.getWebsite() != null && !recruiterProfileRequest.getWebsite().isBlank()){
            recruiterProfile.setWebsite(recruiterProfileRequest.getWebsite());
        }

        recruiterProfileRepository.save(recruiterProfile);

    }

    public DeveloperProfileResponse getDeveloperProfile(String email, Long id) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User Not Found"));

        DeveloperProfile profile = developerProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profile does not exist with this id"));

        List<String> skillNames = profile.getSkills() != null
                ? profile.getSkills().stream().map(Skill::getName).toList()
                : List.of();

        return DeveloperProfileResponse.builder()
                .id(profile.getId())
                .fullName(profile.getFullName())
                .bio(profile.getBio())
                .location(profile.getLocation())
                .yearsExp(profile.getYearsExp())
                .resumeUrl(profile.getResumeUrl())
                .linkedinUrl(profile.getLinkedinUrl())
                .skills(skillNames)
                .build();

    }
}
