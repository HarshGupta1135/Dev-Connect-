package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.RecruiterProfileRequest;
import com.example.DevConnect.entity.RecruiterProfile;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.repository.RecruiterProfileRepository;
import com.example.DevConnect.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecruiterProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;

    public String createProfile(RecruiterProfileRequest recruiterProfileRequest, String email) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User Not Found"));

        if(recruiterProfileRepository.findByUser(user).isPresent()){
            throw new RuntimeException("Profile already exists for this user");
        }

        RecruiterProfile recruiterProfile = RecruiterProfile.builder()
                .user(user)
                .companyName(recruiterProfileRequest.getCompanyName())
                .companyDesc(recruiterProfileRequest.getDescription())
                .website(recruiterProfileRequest.getWebsite())
                .location(recruiterProfileRequest.getLocation())
                .build();

        recruiterProfileRepository.save(recruiterProfile);

        return "Profile Created Successfully";

    }
}
