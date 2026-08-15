package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.DeveloperProfileRequest;
import com.example.DevConnect.dto.response.DeveloperProfileResponse;
import com.example.DevConnect.entity.DeveloperProfile;
import com.example.DevConnect.entity.Skill;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.exception.ResourceNotFoundException;
import com.example.DevConnect.repository.DeveloperProfileRepository;
import com.example.DevConnect.repository.SkillRepository;
import com.example.DevConnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User Not Found"));

        if(developerProfileRepository.findByUser(user).isPresent()){
            throw new RuntimeException("Profile already exists for this user");
        }

        Set<Skill> skillSet = new HashSet<>();
        if (developerProfileRequest.getSkills() != null) {
            for (String skillName : developerProfileRequest.getSkills()) {
                Skill skill = skillRepository.findByName(skillName)
                        .orElseThrow(() -> new RuntimeException("Skill not found: " + skillName + ". Please contact admin to add it."));
                skillSet.add(skill);
            }
        }

        DeveloperProfile profile = DeveloperProfile.builder()
                .user(user)
                .fullName(developerProfileRequest.getFullName())
                .bio(developerProfileRequest.getBio())
                .location(developerProfileRequest.getLocation())
                .yearsExp(developerProfileRequest.getYearsExp())
                .linkedinUrl(developerProfileRequest.getLinkedinUrl())
                .phone(developerProfileRequest.getPhone())
                .address(developerProfileRequest.getAddress())
                .city(developerProfileRequest.getCity())
                .pincode(developerProfileRequest.getPincode())
                .skills(skillSet)
                .build();

        developerProfileRepository.save(profile);

        return "Profile Saved Successfully";

    }

    @Transactional
    public String updateProfile(DeveloperProfileRequest developerProfileRequest, String email) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User Not Found"));

        DeveloperProfile profile = developerProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Developer Profile Not Found"));

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

        if (developerProfileRequest.getPhone() != null) {
            profile.setPhone(developerProfileRequest.getPhone());
        }

        if (developerProfileRequest.getAddress() != null) {
            profile.setAddress(developerProfileRequest.getAddress());
        }

        if (developerProfileRequest.getCity() != null) {
            profile.setCity(developerProfileRequest.getCity());
        }

        if (developerProfileRequest.getPincode() != null) {
            profile.setPincode(developerProfileRequest.getPincode());
        }

        if (developerProfileRequest.getSkills() != null) {
            Set<Skill> skillSet = new HashSet<>();
            for (String skillName : developerProfileRequest.getSkills()) {
                Skill skill = skillRepository.findByName(skillName)
                        .orElseThrow(() -> new RuntimeException("Skill not found: " + skillName + ". Please contact admin to add it."));
                skillSet.add(skill);
            }
            profile.setSkills(skillSet);
        }

        developerProfileRepository.save(profile);

        return "Profile Updated Successfully";

    }


    public DeveloperProfileResponse getProfile(String email) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User Not Found"));

        DeveloperProfile profile = developerProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile Not Found"));

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
                .phone(profile.getPhone())
                .address(profile.getAddress())
                .city(profile.getCity())
                .pincode(profile.getPincode())
                .skills(skillNames)
                .build();

    }

    @Transactional
    public void updateResumeUrl(String email, String resumeUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User Not Found"));
        
        DeveloperProfile profile = developerProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Developer Profile Not Found"));
        
        profile.setResumeUrl(resumeUrl);
        developerProfileRepository.save(profile);
    }
}
