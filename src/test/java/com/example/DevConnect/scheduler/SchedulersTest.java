package com.example.DevConnect.scheduler;

import com.example.DevConnect.config.DataInitializer;
import com.example.DevConnect.entity.Application;
import com.example.DevConnect.entity.DeveloperProfile;
import com.example.DevConnect.entity.JobPosting;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.enums.ApplicationStatus;
import com.example.DevConnect.enums.JobStatus;
import com.example.DevConnect.repository.ApplicationRepository;
import com.example.DevConnect.repository.JobPostingRepository;
import com.example.DevConnect.repository.UserRepository;
import com.example.DevConnect.service.EmailService;
import com.example.DevConnect.service.JobPostingService;
import com.example.DevConnect.service.JobSchedulerService;
import com.example.DevConnect.support.TestData;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Scheduled jobs and the startup admin seed. */
class SchedulersTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class StatusMailTest {

        @Mock
        private EmailService emailService;
        @Mock
        private ApplicationRepository applicationRepository;
        @InjectMocks
        private StatusMail statusMail;

        private Application pending(ApplicationStatus status, String fullName) {
            User user = TestData.user(1L, "dev@gmail.com", "DEVELOPER");
            DeveloperProfile dev = TestData.completeDeveloper(2L, user);
            dev.setFullName(fullName);
            JobPosting job = TestData.job(3L, TestData.recruiter(4L, TestData.user(5L, "r@gmail.com", "RECRUITER"), "Acme"),
                    JobStatus.ACTIVE, null);
            return TestData.application(6L, dev, job, status);
        }

        @Test
        void rejectedApplicantsAreEmailedOnceAndMarkedSent() {
            Application app = pending(ApplicationStatus.REJECTED, "Dev Eloper");
            when(applicationRepository.findByStatusAndMailSent(ApplicationStatus.REJECTED, false)).thenReturn(List.of(app));

            statusMail.sendMailToRejectedApplicants();

            verify(emailService).sendStatusUpdateEmail("dev@gmail.com", "Dev Eloper", "Backend Engineer 3", "REJECTED");
            assertTrue(app.isMailSent());
            verify(applicationRepository).save(app);
        }

        @Test
        void shortlistedApplicantsWithoutANameAreAddressedByUsername() {
            Application app = pending(ApplicationStatus.SHORTLISTED, null);
            when(applicationRepository.findByStatusAndMailSent(ApplicationStatus.SHORTLISTED, false)).thenReturn(List.of(app));

            statusMail.sendMailToShortlistedApplicants();

            verify(emailService).sendStatusUpdateEmail("dev@gmail.com", "dev", "Backend Engineer 3", "SHORTLISTED");
            assertTrue(app.isMailSent());
        }

        @Test
        void nothingPendingSendsNothing() {
            when(applicationRepository.findByStatusAndMailSent(any(), eq(false))).thenReturn(List.of());

            statusMail.sendMailToRejectedApplicants();
            statusMail.sendMailToShortlistedApplicants();

            verify(emailService, never()).sendStatusUpdateEmail(any(), any(), any(), any());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class JobSchedulerServiceUnitTest {

        @Mock
        private JobPostingRepository jobPostingRepository;
        @Mock
        private JobPostingService jobPostingService;
        @InjectMocks
        private JobSchedulerService scheduler;

        @Test
        void closesExpiredJobsAndEvictsTheListingCache() {
            JobPosting expired = TestData.job(1L, null, JobStatus.ACTIVE, TestData.daysFromNow(-1));
            when(jobPostingRepository.findByStatusAndExpiresAtBefore(eq(JobStatus.ACTIVE), any())).thenReturn(List.of(expired));

            scheduler.closeExpiredJobs();

            assertEquals(JobStatus.CLOSED, expired.getStatus());
            verify(jobPostingRepository).saveAll(List.of(expired));
            verify(jobPostingService).evictJobListingsCache();
        }

        @Test
        void nothingExpiredLeavesTheCacheAlone() {
            when(jobPostingRepository.findByStatusAndExpiresAtBefore(eq(JobStatus.ACTIVE), any())).thenReturn(List.of());

            scheduler.closeExpiredJobs();

            verify(jobPostingRepository, never()).saveAll(any());
            verify(jobPostingService, never()).evictJobListingsCache();
        }

        @Test
        void startupSweepNeverStopsTheAppFromBooting() {
            when(jobPostingRepository.findByStatusAndExpiresAtBefore(any(), any())).thenThrow(new RuntimeException("db cold"));

            assertDoesNotThrow(() -> scheduler.closeExpiredJobsOnStartup());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class DataInitializerTest {

        @Mock
        private UserRepository userRepository;
        @Mock
        private PasswordEncoder passwordEncoder;
        @InjectMocks
        private DataInitializer initializer;

        private void configure(String email, String password) {
            ReflectionTestUtils.setField(initializer, "adminUsername", "admin");
            ReflectionTestUtils.setField(initializer, "adminEmail", email);
            ReflectionTestUtils.setField(initializer, "adminPassword", password);
        }

        @Test
        void noCredentialsMeansNoAdmin() throws Exception {
            configure("", "");

            initializer.run();

            verify(userRepository, never()).save(any());
        }

        @Test
        void existingAdminIsLeftAlone() throws Exception {
            configure("admin@gmail.com", "secret");
            when(userRepository.findByUserName("admin")).thenReturn(new User());

            initializer.run();

            verify(userRepository, never()).save(any());
        }

        @Test
        void configuredAdminIsSeededWithEncodedPassword() throws Exception {
            configure("admin@gmail.com", "secret");
            when(userRepository.findByUserName("admin")).thenReturn(null);
            when(userRepository.findByEmail("admin@gmail.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("secret")).thenReturn("ENCODED");

            initializer.run();

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(saved.capture());
            assertEquals("admin@gmail.com", saved.getValue().getEmail());
            assertEquals("ENCODED", saved.getValue().getPassword());
            assertEquals(List.of("ADMIN", "USER"), saved.getValue().getRole());
        }
    }
}
