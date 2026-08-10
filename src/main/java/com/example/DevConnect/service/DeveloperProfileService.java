package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.DeveloperProfileRequest;
import com.example.DevConnect.dto.response.DeveloperProfileResponse;
import com.example.DevConnect.entity.DeveloperProfile;
import com.example.DevConnect.entity.Skill;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.exception.BadRequestException;
import com.example.DevConnect.exception.DuplicateResourceException;
import com.example.DevConnect.exception.ResourceNotFoundException;
import com.example.DevConnect.repository.DeveloperProfileRepository;
import com.example.DevConnect.repository.SkillRepository;
import com.example.DevConnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DeveloperProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeveloperProfileRepository developerProfileRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Transactional
    public String createProfile(DeveloperProfileRequest developerProfileRequest, String email) {

        // Required only when creating: the update endpoint reuses this DTO for partial updates.
        if (developerProfileRequest.getFullName() == null || developerProfileRequest.getFullName().isBlank()) {
            throw new BadRequestException("Full name is required");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        if (developerProfileRepository.findByUser(user).isPresent()) {
            throw new DuplicateResourceException("Profile already exists for this user");
        }

        DeveloperProfile profile = DeveloperProfile.builder()
                .user(user)
                .fullName(developerProfileRequest.getFullName())
                .bio(developerProfileRequest.getBio())
                .location(developerProfileRequest.getLocation())
                .yearsExp(developerProfileRequest.getYearsExp())
                .linkedinUrl(developerProfileRequest.getLinkedinUrl())
                .skills(resolveSkills(developerProfileRequest.getSkills()))
                .build();

        developerProfileRepository.save(profile);

        return "Profile Saved Successfully";

    }

    @Transactional
    public String updateProfile(DeveloperProfileRequest developerProfileRequest, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        DeveloperProfile profile = developerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Developer Profile Not Found"));

        if (developerProfileRequest.getFullName() != null && !developerProfileRequest.getFullName().isBlank()) {
            profile.setFullName(developerProfileRequest.getFullName());
        }

        if (developerProfileRequest.getBio() != null) {
            profile.setBio(developerProfileRequest.getBio());
        }

        if (developerProfileRequest.getLocation() != null && !developerProfileRequest.getLocation().isBlank()) {
            profile.setLocation(developerProfileRequest.getLocation());
        }

        if (developerProfileRequest.getYearsExp() != null) {
            profile.setYearsExp(developerProfileRequest.getYearsExp());
        }

        if (developerProfileRequest.getLinkedinUrl() != null) {
            profile.setLinkedinUrl(developerProfileRequest.getLinkedinUrl());
        }

        if (developerProfileRequest.getSkills() != null) {
            profile.setSkills(resolveSkills(developerProfileRequest.getSkills()));
        }

        developerProfileRepository.save(profile);

        return "Profile Updated Successfully";

    }

    @Transactional(readOnly = true)
    public DeveloperProfileResponse getProfile(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        DeveloperProfile profile = developerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile Not Found"));

        return mapToResponse(profile);

    }

    @Transactional
    public void updateResumeUrl(String email, String resumeUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        DeveloperProfile profile = developerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Developer Profile Not Found"));

        profile.setResumeUrl(resumeUrl);
        developerProfileRepository.save(profile);
    }

    /** Shared by the developer's own view and the recruiter's view of a candidate. */
    public static DeveloperProfileResponse mapToResponse(DeveloperProfile profile) {
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
}
