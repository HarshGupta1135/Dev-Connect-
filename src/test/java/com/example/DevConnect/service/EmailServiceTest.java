package com.example.DevConnect.service;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The SMTP path, which runs whenever no Brevo API key is configured. The Brevo
 * path calls a real HTTPS API and is not exercised here.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void useSmtp() {
        ReflectionTestUtils.setField(emailService, "brevoApiKey", "");
        ReflectionTestUtils.setField(emailService, "senderEmail", "");
        ReflectionTestUtils.setField(emailService, "senderName", "DevConnect");
        ReflectionTestUtils.setField(emailService, "mailUsername", "app@gmail.com");
    }

    private MimeMessage sentMime() {
        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(sent.capture());
        return sent.getValue();
    }

    /** The decoded HTML body (not the wire format, which may be transfer-encoded). */
    private static String body(MimeMessage message) throws Exception {
        message.saveChanges();
        return (String) message.getContent();
    }

    private void stubMime() {
        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void plainEmailGoesOutAsSimpleMessage() {
        emailService.sendEmail("to@gmail.com", "Hi", "Body");

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(sent.capture());
        assertArrayEquals(new String[]{"to@gmail.com"}, sent.getValue().getTo());
        assertEquals("Hi", sent.getValue().getSubject());
        assertEquals("Body", sent.getValue().getText());
    }

    @Test
    void mailFailuresAreSwallowedSoCallersNeverBreak() {
        doThrow(new MailSendException("smtp down")).when(javaMailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> emailService.sendEmail("to@gmail.com", "Hi", "Body"));
    }

    @Test
    void welcomeEmailIsHtmlFromTheMailAccountWithTheUsername() throws Exception {
        stubMime();

        emailService.sendWelcomeEmail("new@gmail.com", "harsh");

        MimeMessage message = sentMime();
        assertEquals("Welcome to DevConnect!", message.getSubject());
        Address[] to = message.getRecipients(Message.RecipientType.TO);
        assertEquals("new@gmail.com", ((InternetAddress) to[0]).getAddress());
        InternetAddress from = (InternetAddress) message.getFrom()[0];
        assertEquals("app@gmail.com", from.getAddress());
        assertEquals("DevConnect", from.getPersonal());
        assertTrue(body(message).contains("Welcome to DevConnect, harsh!"));
    }

    @Test
    void configuredSenderAddressWinsOverTheMailAccount() throws Exception {
        ReflectionTestUtils.setField(emailService, "senderEmail", "noreply@devconnect.dev");
        stubMime();

        emailService.sendWelcomeEmail("new@gmail.com", "harsh");

        assertEquals("noreply@devconnect.dev", ((InternetAddress) sentMime().getFrom()[0]).getAddress());
    }

    @Test
    void statusUpdateEmailNamesTheJob() throws Exception {
        stubMime();

        emailService.sendStatusUpdateEmail("dev@gmail.com", "Dev", "Backend Engineer", "SHORTLISTED");

        MimeMessage message = sentMime();
        assertEquals("Application Update: Backend Engineer", message.getSubject());
        assertTrue(body(message).contains("Dev"));
    }

    @Test
    void applicationConfirmationNamesJobAndCompany() throws Exception {
        stubMime();

        emailService.sendApplicationConfirmationEmail("dev@gmail.com", "Dev", "Backend Engineer", "Acme");

        MimeMessage message = sentMime();
        assertEquals("Application Received: Backend Engineer", message.getSubject());
        assertTrue(body(message).contains("Acme"));
    }

    @Test
    void markupTypedByARecruiterIsEscapedNotRendered() throws Exception {
        stubMime();

        emailService.sendApplicationConfirmationEmail("dev@gmail.com", "<b>Dev</b>",
                "Engineer <img src=x onerror=alert(1)>", "<a href=\"https://evil.example\">Acme</a>");

        String html = body(sentMime());
        assertFalse(html.contains("<a href=\"https://evil.example\">"), html);
        assertFalse(html.contains("<img src=x"), html);
        assertFalse(html.contains("<b>Dev</b>"), html);
        assertTrue(html.contains("&lt;a href=&quot;https://evil.example&quot;&gt;Acme&lt;/a&gt;"), html);
        assertTrue(html.contains("Engineer &lt;img src=x onerror=alert(1)&gt;"), html);
    }

    @Test
    void statusAndWelcomeEmailsEscapeTooButKeepTheirOwnMarkup() throws Exception {
        stubMime();

        emailService.sendStatusUpdateEmail("dev@gmail.com", "Tom & Jerry", "<script>x</script>", "SHORTLISTED");

        String html = body(sentMime());
        assertTrue(html.contains("Hi Tom &amp; Jerry,"), html);
        assertTrue(html.contains("&lt;script&gt;x&lt;/script&gt;"), html);
        assertTrue(html.contains("<div class=\"greeting\">"), "the template itself is still HTML");
        assertTrue(html.contains("status-shortlisted"), "the shortlisted styling still applies");
    }

    @Test
    void missingCompanyNameIsBlankNotTheWordNull() throws Exception {
        stubMime();

        emailService.sendApplicationConfirmationEmail("dev@gmail.com", "Dev", "Engineer", null);

        assertFalse(body(sentMime()).contains(">null<"));
    }

    @Test
    void htmlMailFailuresAreSwallowed() {
        stubMime();
        doThrow(new MailSendException("smtp down")).when(javaMailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> emailService.sendWelcomeEmail("new@gmail.com", "harsh"));
    }
}
