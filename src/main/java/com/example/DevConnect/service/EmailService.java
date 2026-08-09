package com.example.DevConnect.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Async
    public void sendEmail(String to, String subject, String body){
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(to);
            mail.setSubject(subject);
            mail.setText(body);
            javaMailSender.send(mail);
        }catch (Exception e){
            log.error("Exception while sending mail", e);
        }
    }

    @Async
    public void sendStatusUpdateEmail(String toEmail, String developerName, String jobTitle, String newStatus) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String subject = "Application Update: " + jobTitle;
            
            String statusClass = newStatus.equalsIgnoreCase("SHORTLISTED") ? "status-shortlisted" : "status-rejected";
            String statusMessage = newStatus.equalsIgnoreCase("SHORTLISTED") 
                ? "Congratulations! We are pleased to inform you that you have been shortlisted for this role. The recruitment team will contact you shortly with the next steps of the selection process."
                : "After reviewing your application, we have decided not to move forward with your candidacy for this position. We appreciate the time and effort you invested in applying.";

            String htmlMsg = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <style>\n" +
                    "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7f6; margin: 0; padding: 0; color: #333333; }\n" +
                    "        .container { max-width: 600px; margin: 30px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05); }\n" +
                    "        .header { background: linear-gradient(135deg, #4f46e5, #06b6d4); padding: 30px 20px; text-align: center; color: white; }\n" +
                    "        .header h1 { margin: 0; font-size: 24px; font-weight: 600; letter-spacing: 0.5px; }\n" +
                    "        .content { padding: 30px 25px; line-height: 1.6; }\n" +
                    "        .greeting { font-size: 18px; font-weight: 600; color: #1e1b4b; margin-bottom: 20px; }\n" +
                    "        .message { font-size: 15px; color: #4b5563; margin-bottom: 25px; }\n" +
                    "        .status-badge { display: inline-block; padding: 6px 16px; border-radius: 50px; font-weight: bold; font-size: 14px; text-transform: uppercase; }\n" +
                    "        .status-shortlisted { background-color: #d1fae5; color: #065f46; }\n" +
                    "        .status-rejected { background-color: #fee2e2; color: #991b1b; }\n" +
                    "        .job-details { background-color: #f9fafb; border-left: 4px solid #4f46e5; padding: 15px; margin: 20px 0; border-radius: 0 6px 6px 0; }\n" +
                    "        .job-title { font-weight: 600; color: #1f2937; }\n" +
                    "        .footer { background-color: #f3f4f6; text-align: center; padding: 20px; font-size: 12px; color: #9ca3af; border-top: 1px solid #e5e7eb; }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"container\">\n" +
                    "        <div class=\"header\">\n" +
                    "            <h1>DevConnect</h1>\n" +
                    "        </div>\n" +
                    "        <div class=\"content\">\n" +
                    "            <div class=\"greeting\">Hi " + developerName + ",</div>\n" +
                    "            <div class=\"message\">\n" +
                    "                Thank you for your interest in joining our team! We have reviewed your application.\n" +
                    "            </div>\n" +
                    "            <div class=\"job-details\">\n" +
                    "                <strong>Position:</strong> <span class=\"job-title\">" + jobTitle + "</span><br/>\n" +
                    "                <strong>Status:</strong> <span class=\"status-badge " + statusClass + "\">" + newStatus + "</span>\n" +
                    "            </div>\n" +
                    "            <div class=\"message\">\n" +
                    "                " + statusMessage + "\n" +
                    "            </div>\n" +
                    "            <p>Best regards,<br/><strong>DevConnect Recruitment Team</strong></p>\n" +
                    "        </div>\n" +
                    "        <div class=\"footer\">\n" +
                    "            &copy; 2026 DevConnect. All rights reserved.\n" +
                    "        </div>\n" +
                    "    </div>\n" +
                    "</body>\n" +
                    "</html>";

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlMsg, true);

            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Exception while sending HTML mail", e);
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String subject = "Welcome to DevConnect!";
            
            String htmlMsg = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <style>\n" +
                    "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7f6; margin: 0; padding: 40px 0; color: #333333; }\n" +
                    "        .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05); border: 1px solid #e5e7eb; }\n" +
                    "        .header { background: linear-gradient(135deg, #4f46e5, #06b6d4); padding: 40px 20px; text-align: center; color: white; }\n" +
                    "        .header h1 { margin: 0; font-size: 28px; font-weight: 700; letter-spacing: 0.5px; }\n" +
                    "        .content { padding: 40px 35px; line-height: 1.6; }\n" +
                    "        .greeting { font-size: 20px; font-weight: 600; color: #1e1b4b; margin-bottom: 20px; }\n" +
                    "        .message { font-size: 15px; color: #4b5563; margin-bottom: 25px; }\n" +
                    "        .footer { background-color: #f3f4f6; text-align: center; padding: 25px; font-size: 12px; color: #9ca3af; border-top: 1px solid #e5e7eb; }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"container\">\n" +
                    "        <div class=\"header\">\n" +
                    "            <h1>DevConnect</h1>\n" +
                    "        </div>\n" +
                    "        <div class=\"content\">\n" +
                    "            <div class=\"greeting\">Welcome to DevConnect, " + userName + "!</div>\n" +
                    "            <div class=\"message\">\n" +
                    "                We are thrilled to have you join our developer and recruiter community. DevConnect is designed to help you showcase your skills, search for opportunities, and connect with top talent.\n" +
                    "            </div>\n" +
                    "            <div class=\"message\">\n" +
                    "                Log in now to complete your profile and start exploring jobs or matching skills!\n" +
                    "            </div>\n" +
                    "            <p style=\"margin-top: 30px; font-size: 15px; color: #4b5563;\">Best regards,<br/><strong style=\"color: #1f2937;\">DevConnect Team</strong></p>\n" +
                    "        </div>\n" +
                    "        <div class=\"footer\">\n" +
                    "            &copy; 2026 DevConnect. All rights reserved.\n" +
                    "        </div>\n" +
                    "    </div>\n" +
                    "</body>\n" +
                    "</html>";

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlMsg, true);

            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Exception while sending welcome email", e);
        }
    }

    @Async
    public void sendApplicationConfirmationEmail(String toEmail, String developerName, String jobTitle, String companyName) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String subject = "Application Received: " + jobTitle;
            
            String htmlMsg = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <style>\n" +
                    "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7f6; margin: 0; padding: 40px 0; color: #333333; }\n" +
                    "        .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05); border: 1px solid #e5e7eb; }\n" +
                    "        .header { background: linear-gradient(135deg, #4f46e5, #06b6d4); padding: 40px 20px; text-align: center; color: white; }\n" +
                    "        .header h1 { margin: 0; font-size: 28px; font-weight: 700; letter-spacing: 0.5px; }\n" +
                    "        .content { padding: 40px 35px; line-height: 1.6; }\n" +
                    "        .greeting { font-size: 20px; font-weight: 600; color: #1e1b4b; margin-bottom: 20px; }\n" +
                    "        .message { font-size: 15px; color: #4b5563; margin-bottom: 25px; }\n" +
                    "        .job-details { background-color: #f9fafb; border-left: 4px solid #06b6d4; padding: 20px; margin: 25px 0; border-radius: 0 8px 8px 0; }\n" +
                    "        .job-title { font-weight: 600; color: #1f2937; }\n" +
                    "        .footer { background-color: #f3f4f6; text-align: center; padding: 25px; font-size: 12px; color: #9ca3af; border-top: 1px solid #e5e7eb; }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class=\"container\">\n" +
                    "        <div class=\"header\">\n" +
                    "            <h1>DevConnect</h1>\n" +
                    "        </div>\n" +
                    "        <div class=\"content\">\n" +
                    "            <div class=\"greeting\">Hi " + developerName + ",</div>\n" +
                    "            <div class=\"message\">\n" +
                    "                Your application has been successfully submitted! The hiring team has been notified.\n" +
                    "            </div>\n" +
                    "            <div class=\"job-details\">\n" +
                    "                <strong style=\"color: #4b5563;\">Position:</strong> <span class=\"job-title\">" + jobTitle + "</span><br/>\n" +
                    "                <strong style=" + '"' + "color: #4b5563; display: inline-block; margin-top: 8px;" + '"' + ">Company:</strong> <span style=" + '"' + "color: #1f2937; margin-top: 8px; font-weight: 600;" + '"' + ">" + companyName + "</span>\n" +
                    "            </div>\n" +
                    "            <div class=\"message\">\n" +
                    "                We will keep you updated once the recruiter reviews your application and changes your status.\n" +
                    "            </div>\n" +
                    "            <p style=\"margin-top: 30px; font-size: 15px; color: #4b5563;\">Best regards,<br/><strong style=\"color: #1f2937;\">DevConnect Team</strong></p>\n" +
                    "        </div>\n" +
                    "        <div class=\"footer\">\n" +
                    "            &copy; 2026 DevConnect. All rights reserved.\n" +
                    "        </div>\n" +
                    "    </div>\n" +
                    "</body>\n" +
                    "</html>";

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlMsg, true);

            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Exception while sending application confirmation email", e);
        }
    }
}
