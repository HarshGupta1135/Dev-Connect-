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
            
            // Get job role and company name
            String role = app.getJob().getTitle();
            String companyName = app.getJob().getRecruiter().getCompanyName();
            
            String candidateName = app.getDeveloper().getFullName();
            if (candidateName == null || candidateName.trim().isEmpty()) {
                candidateName = user.getUserName();
            }
            if (candidateName == null || candidateName.trim().isEmpty()) {
                candidateName = "Candidate";
            }

            // Construct subject and body
            String subject = "Application Update: " + role + " at " + companyName;

            String bodyTemplate = "Dear [CandidateName],\n\n" +
                                  "Thank you for your interest in the [Role] position at XYZ.\n\n" +
                                  "After reviewing your application, we have decided not to move forward with your candidacy for this position. We appreciate the time and effort you invested in the application process.\n\n" +
                                  "We wish you the best in your future endeavors.\n\n" +
                                  "Best regards,\n" +
                                  "XYZ Recruitment Team";

            // Replace template placeholders
            String body = bodyTemplate
                    .replace("[CandidateName]", candidateName)
                    .replace("[Role]", role)
                    .replace("XYZ", companyName);

            // Send email
            emailService.sendEmail(toEmail, subject, body);

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
            
            // Get job role and company name
            String role = app.getJob().getTitle();
            String companyName = app.getJob().getRecruiter().getCompanyName();
            
            String candidateName = app.getDeveloper().getFullName();
            if (candidateName == null || candidateName.trim().isEmpty()) {
                candidateName = user.getUserName();
            }
            if (candidateName == null || candidateName.trim().isEmpty()) {
                candidateName = "Candidate";
            }

            // Construct subject and body
            String subject = "Application Shortlisted: " + role + " at " + companyName;

            String bodyTemplate = "Dear [CandidateName],\n\n" +
                                  "Congratulations! We are pleased to inform you that you have been shortlisted for the [Role] position at XYZ.\n\n" +
                                  "The recruitment team at XYZ will contact you shortly with the next steps of the selection process.\n\n" +
                                  "Best regards,\n" +
                                  "XYZ Recruitment Team";

            // Replace template placeholders
            String body = bodyTemplate
                    .replace("[CandidateName]", candidateName)
                    .replace("[Role]", role)
                    .replace("XYZ", companyName);

            // Send email
            emailService.sendEmail(toEmail, subject, body);

            // Mark as sent and save to database
            app.setMailSent(true);
            applicationRepository.save(app);
        }
    }
}
