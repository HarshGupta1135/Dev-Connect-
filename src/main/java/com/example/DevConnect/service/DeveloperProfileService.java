package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.DeveloperProfileRequest;
import com.example.DevConnect.entity.DeveloperProfile;
import com.example.DevConnect.entity.Skill;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.repository.DeveloperProfileRepository;
import com.example.DevConnect.repository.SkillRepository;
import com.example.DevConnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class DeveloperProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeveloperProfileRepository developerProfileRepository;

    @Autowired
    private SkillRepository skillRepository;

    public String createProfile(DeveloperProfileRequest developerProfileRequest, String email) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User Not Found"));

        if(developerProfileRepository.findByUser(user).isPresent()){
            throw new RuntimeException("Profile already exists for this user");
        }

        DeveloperProfile profile = new DeveloperProfile();
        profile.setUser(user);
        profile.setFullName(developerProfileRequest.getFullName());
        profile.setBio(developerProfileRequest.getBio());
        profile.setLocation(developerProfileRequest.getLocation());
        profile.setYearsExp(developerProfileRequest.getYearsExp());
        profile.setLinkedinUrl(developerProfileRequest.getLinkedinUrl());

        Set<Skill> skillSet = new HashSet<>();
        if (developerProfileRequest.getSkills() != null) {
            for (String skillName : developerProfileRequest.getSkills()) {
                Skill skill = skillRepository.findByName(skillName)
                        .orElseThrow(() -> new RuntimeException("Skill not found: " + skillName + ". Please contact admin to add it."));
                skillSet.add(skill);
            }
        }

        profile.setSkills(skillSet);

        developerProfileRepository.save(profile);

        return "Profile Saved Successfully";

    }

}
