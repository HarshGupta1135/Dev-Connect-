package com.example.DevConnect.repository;

import com.example.DevConnect.entity.Application;
import com.example.DevConnect.entity.DeveloperProfile;
import com.example.DevConnect.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByDeveloper(DeveloperProfile developer);
    List<Application> findByJob(JobPosting job);
}
