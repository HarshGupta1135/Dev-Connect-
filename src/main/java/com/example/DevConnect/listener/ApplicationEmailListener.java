package com.example.DevConnect.listener;

import com.example.DevConnect.event.ApplicationStatusChangedEvent;
import com.example.DevConnect.event.ApplicationSubmittedEvent;
import com.example.DevConnect.service.ApplicationMailService;
import com.example.DevConnect.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends application emails only once the surrounding database transaction has committed, so a
 * rolled-back application or status change can never produce an email about data that does
 * not exist. Delivery itself runs on the async executor to keep the HTTP response fast.
 */
@Component
@Slf4j
public class ApplicationEmailListener {

    @Autowired
    private EmailService emailService;

    @Autowired
    private ApplicationMailService applicationMailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        try {
            emailService.sendApplicationConfirmationEmail(
                    event.developerEmail(),
                    event.developerName(),
                    event.jobTitle(),
                    event.companyName()
            );
        } catch (Exception e) {
            // A missing confirmation email must not affect the already-saved application.
            log.error("Could not send application confirmation email to {}", event.developerEmail(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationStatusChanged(ApplicationStatusChangedEvent event) {
        try {
            applicationMailService.sendStatusMailAndMark(event.applicationId());
        } catch (Exception e) {
            log.error("Could not send status mail for application {}; the retry scheduler will pick it up",
                    event.applicationId(), e);
        }
    }
}
