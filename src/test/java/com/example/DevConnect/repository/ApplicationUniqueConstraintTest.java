package com.example.DevConnect.repository;

import com.example.DevConnect.entity.*;
import com.example.DevConnect.enums.ApplicationStatus;
import com.example.DevConnect.enums.JobStatus;
import com.example.DevConnect.enums.JobType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Proves the (developer_id, job_id) unique constraint really exists in the schema: the
 * exists-check in ApplicationService is not atomic, so the database has to be the one that
 * refuses a second application for the same job.
 */
@SpringBootTest
@Transactional
public class ApplicationUniqueConstraintTest {

    @Autowired private UserRepository userRepository;
    @Autowired private DeveloperProfileRepository developerProfileRepository;
    @Autowired private RecruiterProfileRepository recruiterProfileRepository;
    @Autowired private JobPostingRepository jobPostingRepository;
    @Autowired private ApplicationRepository applicationRepository;

    @Test
    public void aDeveloperCannotBeStoredTwiceForTheSameJob() {
        User devUser = new User();
        devUser.setUserName("uk_test_dev");
        devUser.setEmail("uk_test_dev@gmail.com");
        devUser.setPassword("irrelevant");
        devUser.setRole(List.of("DEVELOPER"));
        userRepository.save(devUser);

        DeveloperProfile developer = developerProfileRepository.save(
                DeveloperProfile.builder().user(devUser).fullName("UK Test Dev").build());

        User recruiterUser = new User();
        recruiterUser.setUserName("uk_test_rec");
        recruiterUser.setEmail("uk_test_rec@gmail.com");
        recruiterUser.setPassword("irrelevant");
        recruiterUser.setRole(List.of("RECRUITER"));
        userRepository.save(recruiterUser);

        RecruiterProfile recruiter = recruiterProfileRepository.save(
                RecruiterProfile.builder().user(recruiterUser).fullName("UK Test Rec").companyName("UK Corp").build());

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        JobPosting job = jobPostingRepository.save(JobPosting.builder()
                .recruiter(recruiter)
                .title("UK Test Job")
                .description("desc")
                .jobType(JobType.REMOTE)
                .status(JobStatus.ACTIVE)
                .expiresAt(tomorrow.getTime())
                .build());

        applicationRepository.saveAndFlush(Application.builder()
                .developer(developer).job(job).status(ApplicationStatus.APPLIED).build());

        assertThrows(DataIntegrityViolationException.class, () ->
                applicationRepository.saveAndFlush(Application.builder()
                        .developer(developer).job(job).status(ApplicationStatus.APPLIED).build()));
    }
}
