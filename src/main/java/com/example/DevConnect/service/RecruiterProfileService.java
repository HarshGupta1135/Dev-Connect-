package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.RecruiterProfileRequest;
import com.example.DevConnect.dto.response.RecruiterProfileResponse;
import com.example.DevConnect.dto.response.DeveloperProfileResponse;
import com.example.DevConnect.entity.DeveloperProfile;
import com.example.DevConnect.entity.RecruiterProfile;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.exception.BadRequestException;
import com.example.DevConnect.exception.DuplicateResourceException;
import com.example.DevConnect.exception.ResourceNotFoundException;
import com.example.DevConnect.repository.DeveloperProfileRepository;
import com.example.DevConnect.repository.RecruiterProfileRepository;
import com.example.DevConnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // Required only on create; the update endpoint reuses this DTO for partial updates.
        if (recruiterProfileRequest.getFullName() == null || recruiterProfileRequest.getFullName().isBlank()) {
            throw new BadRequestException("Full name is required");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        if (recruiterProfileRepository.findByUser(user).isPresent()) {
            throw new DuplicateResourceException("Profile already exists for this user");
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

    @Transactional(readOnly = true)
    public RecruiterProfileResponse getProfile(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile Not Found"));

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

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile does not exist for this user"));

        if (recruiterProfileRequest.getFullName() != null && !recruiterProfileRequest.getFullName().isBlank()) {
            recruiterProfile.setFullName(recruiterProfileRequest.getFullName());
        }

        if (recruiterProfileRequest.getCompanyName() != null && !recruiterProfileRequest.getCompanyName().isBlank()) {
            recruiterProfile.setCompanyName(recruiterProfileRequest.getCompanyName());
        }

        if (recruiterProfileRequest.getDescription() != null && !recruiterProfileRequest.getDescription().isBlank()) {
            recruiterProfile.setCompanyDesc(recruiterProfileRequest.getDescription());
        }

        if (recruiterProfileRequest.getLocation() != null && !recruiterProfileRequest.getLocation().isBlank()) {
            recruiterProfile.setLocation(recruiterProfileRequest.getLocation());
        }

        if (recruiterProfileRequest.getWebsite() != null && !recruiterProfileRequest.getWebsite().isBlank()) {
            recruiterProfile.setWebsite(recruiterProfileRequest.getWebsite());
        }

        recruiterProfileRepository.save(recruiterProfile);

    }

    @Transactional(readOnly = true)
    public DeveloperProfileResponse getDeveloperProfile(String email, Long id) {

        // Confirms the caller still exists before exposing candidate data.
        userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        DeveloperProfile profile = developerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile does not exist with this id"));

        return DeveloperProfileService.mapToResponse(profile);

    }
}
