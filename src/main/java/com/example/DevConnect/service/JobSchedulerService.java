package com.example.DevConnect.service;

import com.example.DevConnect.entity.JobPosting;
import com.example.DevConnect.enums.JobStatus;
import com.example.DevConnect.repository.JobPostingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class JobSchedulerService {

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private JobPostingService jobPostingService;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void closeExpiredJobs() {
        log.info("Starting scheduled task to close expired job postings...");
        Date now = new Date();
        List<JobPosting> expiredJobs = jobPostingRepository.findByStatusAndExpiresAtBefore(JobStatus.ACTIVE, now);

        if (expiredJobs.isEmpty()) {
            log.info("No expired active job postings found.");
            return;
        }

        for (JobPosting job : expiredJobs) {
            job.setStatus(JobStatus.CLOSED);
        }

        jobPostingRepository.saveAll(expiredJobs);
        log.info("Successfully closed {} expired job postings.", expiredJobs.size());

        jobPostingService.evictJobListingsCache();
        log.info("Successfully evicted job listings cache after scheduler run.");
    }
}
