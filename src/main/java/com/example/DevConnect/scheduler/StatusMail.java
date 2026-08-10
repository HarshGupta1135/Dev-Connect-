package com.example.DevConnect.scheduler;

import com.example.DevConnect.entity.Application;
import com.example.DevConnect.enums.ApplicationStatus;
import com.example.DevConnect.repository.ApplicationRepository;
import com.example.DevConnect.service.ApplicationMailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Safety net for decision emails: applications whose status is final but whose mail never
 * reached the mail server (SMTP down, network blip) still have mailSent = false, so they are
 * retried here. Sending happens in ApplicationMailService, which flags the row only after a
 * successful send, making this loop idempotent.
 */
@Component
@Slf4j
public class StatusMail {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationMailService applicationMailService;

    @Scheduled(cron = "0 */10 * * * *")
    public void retryPendingStatusMails() {
        List<Application> pending = new ArrayList<>();
        pending.addAll(applicationRepository.findByStatusAndMailSent(ApplicationStatus.REJECTED, false));
        pending.addAll(applicationRepository.findByStatusAndMailSent(ApplicationStatus.SHORTLISTED, false));

        if (pending.isEmpty()) {
            return;
        }

        log.info("Retrying {} pending application status mail(s)", pending.size());

        for (Application application : pending) {
            try {
                applicationMailService.sendStatusMailAndMark(application.getId());
            } catch (Exception e) {
                log.error("Retry failed for application {}; will try again on the next run",
                        application.getId(), e);
            }
        }
    }
}
