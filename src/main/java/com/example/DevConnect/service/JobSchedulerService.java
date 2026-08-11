package com.example.DevConnect.service;

import com.example.DevConnect.entity.JobPosting;
import com.example.DevConnect.enums.JobStatus;
import com.example.DevConnect.repository.JobPostingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
        closeExpired("scheduled");
    }

    /**
     * The cron above only fires if the application happens to be running at
     * midnight, so a listing whose closing date passed while the app was down stays
     * ACTIVE indefinitely — the row then contradicts the API, which already refuses
     * applications to an expired posting. Sweeping once on startup closes that gap.
     *
     * Failures here are logged rather than thrown: a cold database or cache is not a
     * reason to refuse to boot, and the next run will catch up.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void closeExpiredJobsOnStartup() {
        try {
            closeExpired("startup");
        } catch (Exception ex) {
            log.warn("Startup sweep of expired job postings failed: {}", ex.getMessage());
        }
    }

    private void closeExpired(String trigger) {
        log.info("Starting {} task to close expired job postings...", trigger);
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
        log.info("Successfully evicted job listings cache after {} run.", trigger);
    }
}
