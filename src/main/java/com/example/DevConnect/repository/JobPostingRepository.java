package com.example.DevConnect.repository;

import com.example.DevConnect.entity.JobPosting;
import com.example.DevConnect.entity.RecruiterProfile;
import com.example.DevConnect.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    List<JobPosting> findByRecruiter(RecruiterProfile recruiter);
    List<JobPosting> findByStatus(JobStatus status);
}
