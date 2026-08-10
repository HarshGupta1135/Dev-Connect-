package com.example.DevConnect.service;

import com.example.DevConnect.entity.Application;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.enums.ApplicationStatus;
import com.example.DevConnect.repository.ApplicationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Single place that sends a status-decision email and records that it was sent.
 * <p>
 * The order matters: the row is only flagged as mailed <em>after</em> the mail server has
 * accepted the message, so a failure leaves mailSent = false and the retry scheduler picks
 * it up on its next run instead of silently dropping the notification.
 */
@Service
@Slf4j
public class ApplicationMailService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EmailService emailService;

    public void sendStatusMailAndMark(Long applicationId) {
        Application application = applicationRepository.findById(applicationId).orElse(null);
        if (application == null) {
            log.warn("Skipping status mail: application {} no longer exists", applicationId);
            return;
        }

        if (application.isMailSent()) {
            return;
        }

        ApplicationStatus status = application.getStatus();
        if (status != ApplicationStatus.SHORTLISTED && status != ApplicationStatus.REJECTED) {
            return;
        }

        User applicant = application.getDeveloper().getUser();
        String developerName = resolveDeveloperName(application.getDeveloper().getFullName(), applicant.getUserName());

        emailService.sendStatusUpdateEmail(
                applicant.getEmail(),
                developerName,
                application.getJob().getTitle(),
                status.name()
        );

        applicationRepository.markMailSent(applicationId);
        log.info("Status mail for application {} sent and marked", applicationId);
    }

    public static String resolveDeveloperName(String fullName, String userName) {
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        if (userName != null && !userName.isBlank()) {
            return userName;
        }
        return "Candidate";
    }
}
