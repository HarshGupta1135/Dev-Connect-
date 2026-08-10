package com.example.DevConnect.service;

import com.example.DevConnect.entity.JobPosting;
import com.example.DevConnect.entity.RecruiterProfile;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.enums.JobStatus;
import com.example.DevConnect.enums.JobType;
import com.example.DevConnect.repository.JobPostingRepository;
import com.example.DevConnect.repository.UserRepository;
import com.example.DevConnect.repository.RecruiterProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class JobSchedulerServiceTest {

    @Autowired
    private JobSchedulerService jobSchedulerService;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;

    @Test
    public void testCloseExpiredJobs() {
        // Create recruiter user
        User recruiterUser = new User();
        recruiterUser.setUserName("test_recruiter_sched");
        recruiterUser.setEmail("recruiter_sched@gmail.com");
        recruiterUser.setPassword("password");
        recruiterUser.setRole(List.of("RECRUITER"));
        userRepository.save(recruiterUser);

        // Create recruiter profile
        RecruiterProfile recruiterProfile = RecruiterProfile.builder()
                .user(recruiterUser)
                .fullName("Scheduler Recruiter")
                .companyName("Test Corp")
                .companyDesc("Description")
                .location("New York")
                .website("www.test.com")
                .build();
        recruiterProfileRepository.save(recruiterProfile);

        // Create an expired active job
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1); // Expired yesterday
        Date expiredDate = cal.getTime();

        JobPosting expiredActiveJob = JobPosting.builder()
                .recruiter(recruiterProfile)
                .title("Expired Active Job")
                .description("Desc")
                .jobType(JobType.REMOTE)
                .location("NY")
                .experienceRequired(2)
                .status(JobStatus.ACTIVE)
                .expiresAt(expiredDate)
                .build();
        jobPostingRepository.save(expiredActiveJob);

        // Create a non-expired active job
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 5); // Expires in 5 days
        Date futureDate = cal.getTime();

        JobPosting nonExpiredActiveJob = JobPosting.builder()
                .recruiter(recruiterProfile)
                .title("Non-expired Active Job")
                .description("Desc")
                .jobType(JobType.REMOTE)
                .location("NY")
                .experienceRequired(2)
                .status(JobStatus.ACTIVE)
                .expiresAt(futureDate)
                .build();
        jobPostingRepository.save(nonExpiredActiveJob);

        // Run the scheduler method manually
        jobSchedulerService.closeExpiredJobs();

        // Verify expired job is closed
        JobPosting updatedExpiredJob = jobPostingRepository.findById(expiredActiveJob.getId()).orElseThrow();
        assertEquals(JobStatus.CLOSED, updatedExpiredJob.getStatus(), "Expired active job should have been closed");

        // Verify non-expired job is still active
        JobPosting updatedNonExpiredJob = jobPostingRepository.findById(nonExpiredActiveJob.getId()).orElseThrow();
        assertEquals(JobStatus.ACTIVE, updatedNonExpiredJob.getStatus(), "Non-expired active job should remain active");
    }
}
