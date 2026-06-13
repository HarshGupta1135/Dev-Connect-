package com.example.DevConnect.repository;

import com.example.DevConnect.entity.Application;
import com.example.DevConnect.entity.DeveloperProfile;
import com.example.DevConnect.entity.JobPosting;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByDeveloper(DeveloperProfile developer);
    List<Application> findByJob(JobPosting job);
    List<Application> findByStatus(ApplicationStatus status);
    List<Application> findByStatusAndMailSent(ApplicationStatus status, boolean mailSent);
    boolean existsByDeveloperAndJob(DeveloperProfile developer, JobPosting job);
    @Query("SELECT a.developer.user FROM Application a WHERE a.status = com.example.DevConnect.enums.ApplicationStatus.REJECTED")
    public List<User> getRejectedDevelopers();

    @Query("SELECT a.developer.user FROM Application a WHERE a.status = com.example.DevConnect.enums.ApplicationStatus.SHORTLISTED")
    public List<User> getShortlistedDevelopers();

}
