package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.ApplicationRequest;
import com.example.DevConnect.entity.*;
import com.example.DevConnect.enums.ApplicationStatus;
import com.example.DevConnect.enums.JobStatus;
import com.example.DevConnect.event.ApplicationStatusChangedEvent;
import com.example.DevConnect.event.ApplicationSubmittedEvent;
import com.example.DevConnect.exception.BadRequestException;
import com.example.DevConnect.exception.DuplicateApplicationException;
import com.example.DevConnect.exception.UnauthorizedException;
import com.example.DevConnect.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Calendar;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Mockito unit tests for the application rules - no database, Redis or mail server needed.
 */
@ExtendWith(MockitoExtension.class)
public class ApplicationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private DeveloperProfileRepository developerProfileRepository;
    @Mock private JobPostingRepository jobPostingRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private RecruiterProfileRepository recruiterProfileRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private ApplicationService applicationService;

    private User user;
    private DeveloperProfile developerProfile;
    private RecruiterProfile recruiterProfile;
    private JobPosting job;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUserName("dev");
        user.setEmail("dev@gmail.com");

        developerProfile = DeveloperProfile.builder().id(10L).user(user).fullName("Dev One").build();
        recruiterProfile = RecruiterProfile.builder().id(20L).companyName("Test Corp").build();

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        job = JobPosting.builder()
                .id(30L)
                .recruiter(recruiterProfile)
                .title("Backend Engineer")
                .status(JobStatus.ACTIVE)
                .expiresAt(tomorrow.getTime())
                .build();
    }

    private void stubDeveloperLookup() {
        when(userRepository.findByEmail("dev@gmail.com")).thenReturn(Optional.of(user));
        when(developerProfileRepository.findByUser(user)).thenReturn(Optional.of(developerProfile));
        when(jobPostingRepository.findById(30L)).thenReturn(Optional.of(job));
    }

    @Test
    public void applyingSavesTheApplicationAndPublishesTheConfirmationEvent() {
        stubDeveloperLookup();
        when(applicationRepository.existsByDeveloperAndJob(developerProfile, job)).thenReturn(false);

        applicationService.applyForJob("dev@gmail.com", new ApplicationRequest(30L, "Keen to join"));

        ArgumentCaptor<Application> saved = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(saved.capture());
        assertEquals(ApplicationStatus.APPLIED, saved.getValue().getStatus());
        assertFalse(saved.getValue().isMailSent());

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertInstanceOf(ApplicationSubmittedEvent.class, event.getValue());
        ApplicationSubmittedEvent submitted = (ApplicationSubmittedEvent) event.getValue();
        assertEquals("dev@gmail.com", submitted.developerEmail());
        assertEquals("Backend Engineer", submitted.jobTitle());
        assertEquals("Test Corp", submitted.companyName());
    }

    @Test
    public void applyingToAClosedJobIsABadRequest() {
        job.setStatus(JobStatus.CLOSED);
        stubDeveloperLookup();

        assertThrows(BadRequestException.class,
                () -> applicationService.applyForJob("dev@gmail.com", new ApplicationRequest(30L, null)));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    public void applyingToAnExpiredJobIsABadRequest() {
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        job.setExpiresAt(yesterday.getTime());
        stubDeveloperLookup();

        assertThrows(BadRequestException.class,
                () -> applicationService.applyForJob("dev@gmail.com", new ApplicationRequest(30L, null)));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    public void applyingTwiceIsRejected() {
        stubDeveloperLookup();
        when(applicationRepository.existsByDeveloperAndJob(developerProfile, job)).thenReturn(true);

        assertThrows(DuplicateApplicationException.class,
                () -> applicationService.applyForJob("dev@gmail.com", new ApplicationRequest(30L, null)));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    public void statusCanOnlyMoveToShortlistedOrRejected() {
        // Validated before any lookup, so no repository stubbing is needed.
        assertThrows(BadRequestException.class,
                () -> applicationService.setApplicationStatus("rec@gmail.com", 1L, ApplicationStatus.APPLIED));
        assertThrows(BadRequestException.class,
                () -> applicationService.setApplicationStatus("rec@gmail.com", 1L, null));
    }

    @Test
    public void aRecruiterCannotUpdateAnotherRecruitersApplication() {
        User recruiterUser = new User();
        recruiterUser.setId(2L);
        recruiterUser.setEmail("rec@gmail.com");
        RecruiterProfile otherRecruiter = RecruiterProfile.builder().id(99L).build();

        Application application = Application.builder().id(5L).developer(developerProfile).job(job).build();

        when(userRepository.findByEmail("rec@gmail.com")).thenReturn(Optional.of(recruiterUser));
        when(recruiterProfileRepository.findByUser(recruiterUser)).thenReturn(Optional.of(otherRecruiter));
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));

        assertThrows(UnauthorizedException.class,
                () -> applicationService.setApplicationStatus("rec@gmail.com", 5L, ApplicationStatus.SHORTLISTED));
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    public void statusUpdateLeavesMailUnsentAndPublishesTheStatusEvent() {
        User recruiterUser = new User();
        recruiterUser.setId(2L);
        recruiterUser.setEmail("rec@gmail.com");

        Application application = Application.builder()
                .id(5L)
                .developer(developerProfile)
                .job(job)
                .status(ApplicationStatus.APPLIED)
                .mailSent(true) // stale flag from an earlier decision must be reset
                .build();

        when(userRepository.findByEmail("rec@gmail.com")).thenReturn(Optional.of(recruiterUser));
        when(recruiterProfileRepository.findByUser(recruiterUser)).thenReturn(Optional.of(recruiterProfile));
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));

        applicationService.setApplicationStatus("rec@gmail.com", 5L, ApplicationStatus.SHORTLISTED);

        assertEquals(ApplicationStatus.SHORTLISTED, application.getStatus());
        assertFalse(application.isMailSent(), "mail must be marked sent only after a successful send");
        verify(eventPublisher).publishEvent(new ApplicationStatusChangedEvent(5L));
    }
}
