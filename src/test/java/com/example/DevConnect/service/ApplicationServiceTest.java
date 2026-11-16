package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.ApplicationRequest;
import com.example.DevConnect.dto.response.ApplicantSummary;
import com.example.DevConnect.dto.response.ApplicationResponse;
import com.example.DevConnect.entity.Application;
import com.example.DevConnect.entity.DeveloperProfile;
import com.example.DevConnect.entity.JobPosting;
import com.example.DevConnect.entity.RecruiterProfile;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.enums.ApplicationStatus;
import com.example.DevConnect.enums.EmailPreference;
import com.example.DevConnect.enums.JobStatus;
import com.example.DevConnect.exception.BadRequestException;
import com.example.DevConnect.exception.DuplicateApplicationException;
import com.example.DevConnect.exception.ResourceNotFoundException;
import com.example.DevConnect.exception.UnauthorizedException;
import com.example.DevConnect.repository.ApplicationRepository;
import com.example.DevConnect.repository.DeveloperProfileRepository;
import com.example.DevConnect.repository.JobPostingRepository;
import com.example.DevConnect.repository.RecruiterProfileRepository;
import com.example.DevConnect.repository.UserRepository;
import com.example.DevConnect.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DeveloperProfileRepository developerProfileRepository;
    @Mock
    private JobPostingRepository jobPostingRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private RecruiterProfileRepository recruiterProfileRepository;
    @Mock
    private JobPostingService jobPostingService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private ApplicationService service;

    private User devUser;
    private DeveloperProfile developer;
    private User recUser;
    private RecruiterProfile recruiter;
    private JobPosting activeJob;

    @BeforeEach
    void setUp() {
        devUser = TestData.user(1L, "dev@gmail.com", "DEVELOPER");
        developer = TestData.completeDeveloper(11L, devUser);
        recUser = TestData.user(2L, "rec@gmail.com", "RECRUITER");
        recruiter = TestData.recruiter(22L, recUser, "Acme");
        activeJob = TestData.job(33L, recruiter, JobStatus.ACTIVE, TestData.daysFromNow(7));

        when(userRepository.findByEmail("dev@gmail.com")).thenReturn(Optional.of(devUser));
        when(userRepository.findByEmail("rec@gmail.com")).thenReturn(Optional.of(recUser));
        when(developerProfileRepository.findByUser(devUser)).thenReturn(Optional.of(developer));
        when(recruiterProfileRepository.findByUser(recUser)).thenReturn(Optional.of(recruiter));
        when(jobPostingRepository.findById(33L)).thenReturn(Optional.of(activeJob));
    }

    private void apply() {
        service.applyForJob("dev@gmail.com", new ApplicationRequest(33L, "Pick me"));
    }

    // ---------- apply ----------

    @Test
    void applySavesAnAppliedApplicationAndEmailsTheDeveloper() {
        apply();

        ArgumentCaptor<Application> saved = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(saved.capture());
        assertEquals(ApplicationStatus.APPLIED, saved.getValue().getStatus());
        assertEquals("Pick me", saved.getValue().getCoverNote());
        assertEquals(developer, saved.getValue().getDeveloper());
        assertEquals(activeJob, saved.getValue().getJob());
        verify(emailService).sendApplicationConfirmationEmail("dev@gmail.com", "Dev Eloper", "Backend Engineer 33", "Acme");
    }

    @Test
    void applyConfirmationGoesToTheChosenNotificationAddress() {
        devUser.setSecondaryEmail("alerts@example.com");
        devUser.setEmailPreference(EmailPreference.SECONDARY);

        apply();

        verify(emailService).sendApplicationConfirmationEmail(
                "alerts@example.com", "Dev Eloper", "Backend Engineer 33", "Acme");
    }

    @Test
    void applyWithIncompleteProfileListsEveryMissingField() {
        developer.setResumeUrl(null);
        developer.setPhone(" ");
        developer.setPincode(null);

        BadRequestException ex = assertThrows(BadRequestException.class, this::apply);

        assertEquals("Complete your profile before applying. Missing: resume, phone number, pincode", ex.getMessage());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void applyWithoutDeveloperProfileIsNotFound() {
        when(developerProfileRepository.findByUser(devUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, this::apply);
    }

    @Test
    void applyToUnknownJobIsNotFound() {
        when(jobPostingRepository.findById(33L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, this::apply);
    }

    @Test
    void applyToClosedJobIsRejected() {
        activeJob.setStatus(JobStatus.CLOSED);

        RuntimeException ex = assertThrows(RuntimeException.class, this::apply);
        assertEquals("Cannot apply to a closed or inactive job posting", ex.getMessage());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void applyToExpiredJobIsRejected() {
        activeJob.setExpiresAt(TestData.daysFromNow(-1));

        RuntimeException ex = assertThrows(RuntimeException.class, this::apply);
        assertEquals("Cannot apply to an expired job posting", ex.getMessage());
    }

    @Test
    void applyingTwiceIsADuplicate() {
        when(applicationRepository.existsByDeveloperAndJob(developer, activeJob)).thenReturn(true);

        assertThrows(DuplicateApplicationException.class, this::apply);
        verify(applicationRepository, never()).save(any());
        verify(emailService, never()).sendApplicationConfirmationEmail(any(), any(), any(), any());
    }

    // ---------- developer's own applications ----------

    @Test
    void developerSeesTheirApplicationsWithJobTitles() {
        when(applicationRepository.findByDeveloper(developer)).thenReturn(List.of(
                TestData.application(100L, developer, activeJob, ApplicationStatus.APPLIED)));

        List<ApplicationResponse> list = service.getJobApplications("dev@gmail.com");

        assertEquals(1, list.size());
        assertEquals(33L, list.get(0).getJobId());
        assertEquals("Backend Engineer 33", list.get(0).getJobTitle());
        assertEquals(ApplicationStatus.APPLIED, list.get(0).getStatus());
    }

    // ---------- recruiter's view of applicants ----------

    @Test
    void recruiterSeesApplicantDetailsWithSortedSkills() {
        developer.setSkills(TestData.skills(TestData.skill(2L, "SQL"), TestData.skill(1L, "Java")));
        when(applicationRepository.findByJob(activeJob)).thenReturn(List.of(
                TestData.application(100L, developer, activeJob, ApplicationStatus.APPLIED)));

        List<ApplicationResponse> list = service.getAllApplicantsById("rec@gmail.com", 33L);

        ApplicantSummary applicant = list.get(0).getApplicant();
        assertNotNull(applicant);
        assertEquals("dev@gmail.com", applicant.getEmail());
        assertEquals("9999999999", applicant.getPhone());
        assertEquals("https://cdn.example/resume.pdf", applicant.getResumeUrl());
        assertEquals(List.of("Java", "SQL"), applicant.getSkills());
    }

    @Test
    void recruiterCannotSeeApplicantsOfSomeoneElsesJob() {
        RecruiterProfile other = TestData.recruiter(99L, TestData.user(9L, "x@gmail.com", "RECRUITER"), "Other");
        activeJob.setRecruiter(other);

        assertThrows(UnauthorizedException.class, () -> service.getAllApplicantsById("rec@gmail.com", 33L));
        verify(applicationRepository, never()).findByJob(any());
    }

    // ---------- status updates ----------

    @Test
    void shortlistingMarksMailSentAndEmailsTheApplicant() {
        Application app = TestData.application(100L, developer, activeJob, ApplicationStatus.APPLIED);
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(app));

        service.setApplicationStatus("rec@gmail.com", 100L, ApplicationStatus.SHORTLISTED);

        assertEquals(ApplicationStatus.SHORTLISTED, app.getStatus());
        assertTrue(app.isMailSent(), "the scheduler must not send it again");
        verify(applicationRepository).save(app);
        verify(emailService).sendStatusUpdateEmail("dev@gmail.com", "Dev Eloper", "Backend Engineer 33", "SHORTLISTED");
    }

    @Test
    void statusEmailFallsBackToUsernameThenCandidate() {
        Application app = TestData.application(100L, developer, activeJob, ApplicationStatus.APPLIED);
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(app));
        developer.setFullName(" ");

        service.setApplicationStatus("rec@gmail.com", 100L, ApplicationStatus.REJECTED);
        verify(emailService).sendStatusUpdateEmail("dev@gmail.com", "dev", "Backend Engineer 33", "REJECTED");

        devUser.setUserName(null);
        service.setApplicationStatus("rec@gmail.com", 100L, ApplicationStatus.REJECTED);
        verify(emailService).sendStatusUpdateEmail("dev@gmail.com", "Candidate", "Backend Engineer 33", "REJECTED");
    }

    @Test
    void onlyShortlistedOrRejectedAreAllowed() {
        Application app = TestData.application(100L, developer, activeJob, ApplicationStatus.APPLIED);
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(app));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.setApplicationStatus("rec@gmail.com", 100L, ApplicationStatus.APPLIED));
        assertTrue(ex.getMessage().startsWith("Invalid status update"));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void recruiterCannotChangeStatusOnSomeoneElsesJob() {
        RecruiterProfile other = TestData.recruiter(99L, TestData.user(9L, "x@gmail.com", "RECRUITER"), "Other");
        JobPosting othersJob = TestData.job(44L, other, JobStatus.ACTIVE, null);
        when(applicationRepository.findById(100L)).thenReturn(Optional.of(
                TestData.application(100L, developer, othersJob, ApplicationStatus.APPLIED)));

        assertThrows(UnauthorizedException.class,
                () -> service.setApplicationStatus("rec@gmail.com", 100L, ApplicationStatus.SHORTLISTED));
        verify(emailService, never()).sendStatusUpdateEmail(any(), any(), any(), any());
    }

    @Test
    void unknownApplicationIsNotFound() {
        when(applicationRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.setApplicationStatus("rec@gmail.com", 100L, ApplicationStatus.SHORTLISTED));
    }
}
