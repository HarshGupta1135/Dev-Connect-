package com.example.DevConnect.mailTest;

import com.example.DevConnect.service.EmailService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Sends a real email through the configured mail transport. Never runs on its own:
 * tagged manual, and it also needs a recipient passed in explicitly.
 *
 *   mvn test -Dtest=MailTest -Dtest.excludedGroups= -Dmail.test.to=you@gmail.com
 */
@SpringBootTest
@Tag("manual")
class MailTest {

    @Autowired
    private EmailService emailService;

    @Test
    void sendMail() {
        String to = System.getProperty("mail.test.to");
        assumeTrue(to != null && !to.isBlank(), "Set -Dmail.test.to=<address> to send a test email");

        emailService.sendEmail(to, "Testing mail service", "Hii. What's up?");
    }

}
