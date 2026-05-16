package com.example.DevConnect.service;

import com.example.DevConnect.entity.JobPosting;
import com.example.DevConnect.repository.JobPostingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobPostingService {

    @Autowired
    private JobPostingRepository jobPostingRepository;

    public JobPosting createJob(JobPosting jobPosting) {
        return jobPostingRepository.save(jobPosting);
    }

    public List<JobPosting> getAllJobs() {
        return jobPostingRepository.findAll();
    }
}
