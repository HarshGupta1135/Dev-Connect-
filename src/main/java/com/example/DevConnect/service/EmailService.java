package com.example.DevConnect.service;

import com.example.DevConnect.exception.MailDeliveryException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
@Slf4j
public class EmailService {

    private static final String BASE_STYLE = """
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7f6; margin: 0; padding: 40px 0; color: #333333; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05); border: 1px solid #e5e7eb; }
                    .header { background: linear-gradient(135deg, #4f46e5, #06b6d4); padding: 40px 20px; text-align: center; color: white; }
                    .header h1 { margin: 0; font-size: 28px; font-weight: 700; letter-spacing: 0.5px; }
                    .content { padding: 40px 35px; line-height: 1.6; }
                    .greeting { font-size: 20px; font-weight: 600; color: #1e1b4b; margin-bottom: 20px; }
                    .message { font-size: 15px; color: #4b5563; margin-bottom: 25px; }
                    .status-badge { display: inline-block; padding: 6px 16px; border-radius: 50px; font-weight: bold; font-size: 14px; text-transform: uppercase; }
                    .status-shortlisted { background-color: #d1fae5; color: #065f46; }
                    .status-rejected { background-color: #fee2e2; color: #991b1b; }
                    .job-details { background-color: #f9fafb; border-left: 4px solid #4f46e5; padding: 20px; margin: 25px 0; border-radius: 0 8px 8px 0; }
                    .job-title { font-weight: 600; color: #1f2937; }
                    .footer { background-color: #f3f4f6; text-align: center; padding: 25px; font-size: 12px; color: #9ca3af; border-top: 1px solid #e5e7eb; }
            """;

    @Autowired
    private JavaMailSender javaMailSender;

    @Async
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(to);
            mail.setSubject(subject);
            mail.setText(body);
            javaMailSender.send(mail);
        } catch (Exception e) {
            log.error("Exception while sending mail", e);
        }
    }

    /**
     * Sends the shortlist/reject decision. Deliberately synchronous and deliberately throws:
     * the caller records "mail sent" only when this returns normally.
     */
    public void sendStatusUpdateEmail(String toEmail, String developerName, String jobTitle, String newStatus) {
        boolean shortlisted = "SHORTLISTED".equalsIgnoreCase(newStatus);
        String statusClass = shortlisted ? "status-shortlisted" : "status-rejected";
        String statusMessage = shortlisted
                ? "Congratulations! We are pleased to inform you that you have been shortlisted for this role. The recruitment team will contact you shortly with the next steps of the selection process."
                : "After reviewing your application, we have decided not to move forward with your candidacy for this position. We appreciate the time and effort you invested in applying.";

        String html = page("""
                        <div class="greeting">Hi %s,</div>
                        <div class="message">
                            Thank you for your interest in joining our team! We have reviewed your application.
                        </div>
                        <div class="job-details">
                            <strong>Position:</strong> <span class="job-title">%s</span><br/>
                            <strong>Status:</strong> <span class="status-badge %s">%s</span>
                        </div>
                        <div class="message">%s</div>
                        <p>Best regards,<br/><strong>DevConnect Recruitment Team</strong></p>
                """.formatted(escape(developerName), escape(jobTitle), statusClass, escape(newStatus), statusMessage));

        sendHtml(toEmail, "Application Update: " + jobTitle, html);
    }

    /** Confirmation that an application was received; throws so the listener can log a failure. */
    public void sendApplicationConfirmationEmail(String toEmail, String developerName, String jobTitle, String companyName) {
        String html = page("""
                        <div class="greeting">Hi %s,</div>
                        <div class="message">
                            Your application has been successfully submitted! The hiring team has been notified.
                        </div>
                        <div class="job-details">
                            <strong style="color: #4b5563;">Position:</strong> <span class="job-title">%s</span><br/>
                            <strong style="color: #4b5563; display: inline-block; margin-top: 8px;">Company:</strong>
                            <span style="color: #1f2937; margin-top: 8px; font-weight: 600;">%s</span>
                        </div>
                        <div class="message">
                            We will keep you updated once the recruiter reviews your application and changes your status.
                        </div>
                        <p>Best regards,<br/><strong>DevConnect Team</strong></p>
                """.formatted(escape(developerName), escape(jobTitle), escape(companyName)));

        sendHtml(toEmail, "Application Received: " + jobTitle, html);
    }

    /**
     * Welcome mail is a nice-to-have, so it stays fire-and-forget: a mail outage must not
     * fail an otherwise successful registration.
     */
    @Async
    public void sendWelcomeEmail(String toEmail, String userName) {
        String html = page("""
                        <div class="greeting">Welcome to DevConnect, %s!</div>
                        <div class="message">
                            We are thrilled to have you join our developer and recruiter community. DevConnect is designed to help you showcase your skills, search for opportunities, and connect with top talent.
                        </div>
                        <div class="message">
                            Log in now to complete your profile and start exploring jobs or matching skills!
                        </div>
                        <p style="margin-top: 30px; font-size: 15px; color: #4b5563;">Best regards,<br/><strong style="color: #1f2937;">DevConnect Team</strong></p>
                """.formatted(escape(userName)));

        try {
            sendHtml(toEmail, "Welcome to DevConnect!", html);
        } catch (MailDeliveryException e) {
            log.error("Exception while sending welcome email to {}", toEmail, e);
        }
    }

    private void sendHtml(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new MailDeliveryException("Could not send email to " + toEmail, e);
        }
    }

    private String page(String content) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                %s
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>DevConnect</h1>
                        </div>
                        <div class="content">
                %s
                        </div>
                        <div class="footer">
                            &copy; 2026 DevConnect. All rights reserved.
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(BASE_STYLE, content);
    }

    /**
     * User-supplied values (names, job titles, company names) are interpolated into HTML, so
     * they are escaped: an unescaped "&lt;" or a pasted tag would otherwise break or inject
     * markup into the message.
     */
    private String escape(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
