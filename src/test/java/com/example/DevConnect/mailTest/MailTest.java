package com.example.DevConnect.mailTest;

import com.example.DevConnect.service.EmailService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Manual check: this really does deliver mail through the configured SMTP account, so it is
 * disabled by default and must not run as part of the normal build. Enable it locally when
 * verifying mail credentials.
 */
@SpringBootTest
@Disabled("Sends a real email; enable manually when verifying SMTP configuration")
public class MailTest {

    @Autowired
    private EmailService emailService;

    @Test
    public void sendMail() {
        emailService.sendEmail("harshkumar11355@gmail.com",
                "Testing mail service",
                "Hii. What's up?");
    }

}
