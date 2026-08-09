package com.example.DevConnect.scheduler;

import com.example.DevConnect.entity.Application;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.enums.ApplicationStatus;
import com.example.DevConnect.repository.ApplicationRepository;
import com.example.DevConnect.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StatusMail {

    @Autowired
    private EmailService emailService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Scheduled(cron = "0 */10 * * * *")
    public void sendMailToRejectedApplicants() {
        // Retrieve all applications with status REJECTED that haven't received an email yet
        List<Application> rejectedApplications = applicationRepository.findByStatusAndMailSent(ApplicationStatus.REJECTED, false);
        for (Application app : rejectedApplications) {
            // Get candidate user details
            User user = app.getDeveloper().getUser();
            String toEmail = user.getEmail();
            
            // Get job role
            String role = app.getJob().getTitle();
            
            String candidateName = app.getDeveloper().getFullName();
            if (candidateName == null || candidateName.trim().isEmpty()) {
                candidateName = user.getUserName();
            }
            if (candidateName == null || candidateName.trim().isEmpty()) {
                candidateName = "Candidate";
            }

            // Send HTML email
            emailService.sendStatusUpdateEmail(toEmail, candidateName, role, "REJECTED");

            // Mark as sent and save to database
            app.setMailSent(true);
            applicationRepository.save(app);
        }
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void sendMailToShortlistedApplicants() {
        // Retrieve all applications with status SHORTLISTED that haven't received an email yet
        List<Application> shortlistedApplications = applicationRepository.findByStatusAndMailSent(ApplicationStatus.SHORTLISTED, false);
        for (Application app : shortlistedApplications) {
            // Get candidate user details
            User user = app.getDeveloper().getUser();
            String toEmail = user.getEmail();
            
            // Get job role
            String role = app.getJob().getTitle();
            
            String candidateName = app.getDeveloper().getFullName();
            if (candidateName == null || candidateName.trim().isEmpty()) {
                candidateName = user.getUserName();
            }
            if (candidateName == null || candidateName.trim().isEmpty()) {
                candidateName = "Candidate";
            }

            // Send HTML email
            emailService.sendStatusUpdateEmail(toEmail, candidateName, role, "SHORTLISTED");

            // Mark as sent and save to database
            app.setMailSent(true);
            applicationRepository.save(app);
        }
    }
}
