package com.example.DevConnect.repository;

import com.example.DevConnect.entity.Application;
import com.example.DevConnect.entity.DeveloperProfile;
import com.example.DevConnect.entity.JobPosting;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByDeveloper(DeveloperProfile developer);
    List<Application> findByJob(JobPosting job);
    List<Application> findByStatus(ApplicationStatus status);
    List<Application> findByStatusAndMailSent(ApplicationStatus status, boolean mailSent);
    boolean existsByDeveloperAndJob(DeveloperProfile developer, JobPosting job);

    /**
     * Flags a decision mail as delivered in its own short transaction, so the flag is never
     * committed together with (and therefore never ahead of) the actual send.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Application a SET a.mailSent = true WHERE a.id = :id")
    int markMailSent(@Param("id") Long id);

    @Query("SELECT a.developer.user FROM Application a WHERE a.status = com.example.DevConnect.enums.ApplicationStatus.REJECTED")
    List<User> getRejectedDevelopers();

    @Query("SELECT a.developer.user FROM Application a WHERE a.status = com.example.DevConnect.enums.ApplicationStatus.SHORTLISTED")
    List<User> getShortlistedDevelopers();

}
