package com.example.DevConnect.mailTest;

import com.example.DevConnect.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MailTest {

    @Autowired
    private EmailService emailService;

    @Test
    public void sendMail(){
        emailService.sendEmail("harshkumar11355@gmail.com",
                "Testing mail service",
                "Hii. What's up?");
    }

}
