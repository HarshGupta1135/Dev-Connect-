package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.JobPostingRequest;
import com.example.DevConnect.dto.response.CustomPageResponse;
import com.example.DevConnect.dto.response.JobPostingResponse;
import com.example.DevConnect.dto.response.JobPostingWithMatchResponse;
import com.example.DevConnect.entity.DeveloperProfile;
import com.example.DevConnect.entity.JobPosting;
import com.example.DevConnect.entity.RecruiterProfile;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.enums.JobStatus;
import com.example.DevConnect.enums.JobType;
import com.example.DevConnect.exception.ResourceNotFoundException;
import com.example.DevConnect.exception.UnauthorizedException;
import com.example.DevConnect.repository.DeveloperProfileRepository;
import com.example.DevConnect.repository.JobPostingRepository;
import com.example.DevConnect.repository.RecruiterProfileRepository;
import com.example.DevConnect.repository.SkillRepository;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobPostingServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JobPostingRepository jobPostingRepository;
    @Mock
    private RecruiterProfileRepository recruiterProfileRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private DeveloperProfileRepository developerProfileRepository;

    /** Stands in for the Spring proxy the service calls itself through (for @Cacheable). */
    @Mock
    private JobPostingService self;

    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache listingsCache;

    @InjectMocks
    private JobPostingService service;

    private User recruiterUser;
    private RecruiterProfile recruiter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "self", self);
        recruiterUser = TestData.user(1L, "rec@gmail.com", "RECRUITER");
        recruiter = TestData.recruiter(10L, recruiterUser, "Acme");
        when(userRepository.findByEmail("rec@gmail.com")).thenReturn(Optional.of(recruiterUser));
        when(recruiterProfileRepository.findByUser(recruiterUser)).thenReturn(Optional.of(recruiter));
        when(skillRepository.findByName("Java")).thenReturn(Optional.of(TestData.skill(1L, "Java")));
        when(cacheManager.getCache("job-listings")).thenReturn(listingsCache);
    }

    private static JobPostingRequest request() {
        JobPostingRequest r = new JobPostingRequest();
        r.setTitle("Backend Engineer");
        r.setDescription("Spring");
        r.setJobType(JobType.HYBRID);
        r.setLocation("Pune");
        r.setExperienceRequired(3);
        r.setStatus(JobStatus.ACTIVE);
        r.setRequiredSkills(List.of("Java"));
        return r;
    }

    // ---------- create ----------

    @Test
    void createJobDefaultsExpiryToThirtyDays() {
        service.createJob("rec@gmail.com", request());

        ArgumentCaptor<JobPosting> saved = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobPostingRepository).save(saved.capture());
        long days = TimeUnit.MILLISECONDS.toDays(saved.getValue().getExpiresAt().getTime() - System.currentTimeMillis());
        assertTrue(days >= 29 && days <= 30, "expected ~30 days, got " + days);
        assertEquals(recruiter, saved.getValue().getRecruiter());
        assertEquals("Backend Engineer", saved.getValue().getTitle());
        assertEquals(1, saved.getValue().getRequiredSkills().size());
    }

    @Test
    void createJobKeepsAnExplicitExpiry() {
        JobPostingRequest r = request();
        Date expiry = TestData.daysFromNow(5);
        r.setExpiresAt(expiry);

        service.createJob("rec@gmail.com", r);

        ArgumentCaptor<JobPosting> saved = ArgumentCaptor.forClass(JobPosting.class);
        verify(jobPostingRepository).save(saved.capture());
        assertEquals(expiry, saved.getValue().getExpiresAt());
    }

    @Test
    void createJobWithUnknownSkillIsRejected() {
        when(skillRepository.findByName("Cobol")).thenReturn(Optional.empty());
        JobPostingRequest r = request();
        r.setRequiredSkills(List.of("Cobol"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createJob("rec@gmail.com", r));
        assertTrue(ex.getMessage().startsWith("Skill not found: Cobol"));
        verify(jobPostingRepository, never()).save(any());
    }

    @Test
    void createJobWithoutRecruiterProfileIsRejected() {
        when(recruiterProfileRepository.findByUser(recruiterUser)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.createJob("rec@gmail.com", request()));
        verify(jobPostingRepository, never()).save(any());
    }

    // ---------- read ----------

    @Test
    void getJobByIdMapsSkillsAndCompany() {
        JobPosting job = TestData.job(5L, recruiter, JobStatus.ACTIVE, TestData.daysFromNow(3));
        job.setRequiredSkills(TestData.skills(TestData.skill(1L, "Java")));
        when(jobPostingRepository.findById(5L)).thenReturn(Optional.of(job));

        JobPostingResponse response = service.getJobById(5L);

        assertEquals(5L, response.getId());
        assertEquals("Acme", response.getCompanyName());
        assertEquals(List.of("Java"), response.getRequiredSkills());
    }

    @Test
    void getJobByIdForUnknownIdIsNotFound() {
        when(jobPostingRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getJobById(404L));
    }

    @Test
    void getAllJobsByRecruiterMapsEachJob() {
        when(jobPostingRepository.findByRecruiter(recruiter)).thenReturn(List.of(
                TestData.job(1L, recruiter, JobStatus.ACTIVE, null),
                TestData.job(2L, recruiter, JobStatus.CLOSED, null)));

        List<JobPostingResponse> jobs = service.getAllJobsByRecruiter("rec@gmail.com");

        assertEquals(List.of(1L, 2L), jobs.stream().map(JobPostingResponse::getId).toList());
    }

    // ---------- update / close: ownership ----------

    @Test
    void updateJobChangesOnlyProvidedFields() {
        JobPosting job = TestData.job(5L, recruiter, JobStatus.ACTIVE, null);
        when(jobPostingRepository.findById(5L)).thenReturn(Optional.of(job));
        JobPostingRequest partial = new JobPostingRequest();
        partial.setTitle("Senior Backend Engineer");

        service.updateJob("rec@gmail.com", 5L, partial);

        assertEquals("Senior Backend Engineer", job.getTitle());
        assertEquals("Spring Boot", job.getDescription());
        assertEquals(JobStatus.ACTIVE, job.getStatus());
        verify(jobPostingRepository).save(job);
    }

    @Test
    void updateJobReplacesSkillsWhenGiven() {
        JobPosting job = TestData.job(5L, recruiter, JobStatus.ACTIVE, null);
        when(jobPostingRepository.findById(5L)).thenReturn(Optional.of(job));
        JobPostingRequest r = new JobPostingRequest();
        r.setRequiredSkills(List.of("Java"));

        service.updateJob("rec@gmail.com", 5L, r);

        assertEquals(List.of("Java"), job.getRequiredSkills().stream().map(s -> s.getName()).toList());
    }

    @Test
    void updateJobOwnedByAnotherRecruiterIsForbidden() {
        RecruiterProfile other = TestData.recruiter(99L, TestData.user(9L, "other@gmail.com", "RECRUITER"), "Other");
        when(jobPostingRepository.findById(5L)).thenReturn(Optional.of(TestData.job(5L, other, JobStatus.ACTIVE, null)));

        assertThrows(UnauthorizedException.class, () -> service.updateJob("rec@gmail.com", 5L, request()));
        verify(jobPostingRepository, never()).save(any());
    }

    @Test
    void updateUnknownJobIsNotFound() {
        when(jobPostingRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateJob("rec@gmail.com", 5L, request()));
    }

    @Test
    void closeJobMarksItClosed() {
        JobPosting job = TestData.job(5L, recruiter, JobStatus.ACTIVE, null);
        when(jobPostingRepository.findById(5L)).thenReturn(Optional.of(job));

        service.closeJobById("rec@gmail.com", 5L);

        assertEquals(JobStatus.CLOSED, job.getStatus());
    }

    @Test
    void closeJobOwnedByAnotherRecruiterIsForbidden() {
        RecruiterProfile other = TestData.recruiter(99L, TestData.user(9L, "other@gmail.com", "RECRUITER"), "Other");
        JobPosting job = TestData.job(5L, other, JobStatus.ACTIVE, null);
        when(jobPostingRepository.findById(5L)).thenReturn(Optional.of(job));

        assertThrows(UnauthorizedException.class, () -> service.closeJobById("rec@gmail.com", 5L));
        assertEquals(JobStatus.ACTIVE, job.getStatus());
    }

    // ---------- listing cache eviction ----------

    @Test
    void creatingAJobClearsTheListings() {
        service.createJob("rec@gmail.com", request());

        verify(listingsCache).invalidate();
    }

    @Test
    void updatingAJobClearsTheListings() {
        when(jobPostingRepository.findById(5L)).thenReturn(Optional.of(TestData.job(5L, recruiter, JobStatus.ACTIVE, null)));

        service.updateJob("rec@gmail.com", 5L, new JobPostingRequest());

        verify(listingsCache).invalidate();
    }

    @Test
    void closingAJobClearsTheListings() {
        when(jobPostingRepository.findById(5L)).thenReturn(Optional.of(TestData.job(5L, recruiter, JobStatus.ACTIVE, null)));

        service.closeJobById("rec@gmail.com", 5L);

        verify(listingsCache).invalidate();
    }

    @Test
    void forbiddenCloseLeavesTheListingsAlone() {
        RecruiterProfile other = TestData.recruiter(99L, TestData.user(9L, "other@gmail.com", "RECRUITER"), "Other");
        when(jobPostingRepository.findById(5L)).thenReturn(Optional.of(TestData.job(5L, other, JobStatus.ACTIVE, null)));

        assertThrows(UnauthorizedException.class, () -> service.closeJobById("rec@gmail.com", 5L));

        verify(listingsCache, never()).invalidate();
    }

    @Test
    void insideATransactionTheListingsAreClearedOnlyAfterCommit() {
        when(jobPostingRepository.findById(5L)).thenReturn(Optional.of(TestData.job(5L, recruiter, JobStatus.ACTIVE, null)));
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.closeJobById("rec@gmail.com", 5L);
            verify(listingsCache, never()).invalidate();

            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(listingsCache).invalidate();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rolledBackChangeLeavesTheListingsAlone() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.createJob("rec@gmail.com", request());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            verify(listingsCache, never()).invalidate();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // ---------- listing ----------

    @Test
    void anonymousListingAsksTheCacheWithNoDeveloperSkills() {
        Pageable page = PageRequest.of(0, 10);

        service.getActiveJobs(List.of("Java"), "Pune", JobType.REMOTE, page, null);

        verify(self).getCachedActiveJobs(List.of("Java"), "Pune", JobType.REMOTE, page, List.of());
        verify(userRepository, never()).findByEmail("dev@gmail.com");
    }

    @Test
    void developerListingPassesTheirSkillIdsSortedSoTheCacheKeyIsStable() {
        User devUser = TestData.user(2L, "dev@gmail.com", "DEVELOPER");
        DeveloperProfile dev = TestData.completeDeveloper(3L, devUser);
        dev.setSkills(TestData.skills(TestData.skill(5L, "Go"), TestData.skill(1L, "Java"), TestData.skill(3L, "SQL")));
        when(userRepository.findByEmail("dev@gmail.com")).thenReturn(Optional.of(devUser));
        when(developerProfileRepository.findByUser(devUser)).thenReturn(Optional.of(dev));
        Pageable page = PageRequest.of(0, 10);

        service.getActiveJobs(null, null, null, page, "dev@gmail.com");

        verify(self).getCachedActiveJobs(isNull(), isNull(), isNull(), eq(page), eq(List.of(1L, 3L, 5L)));
    }

    @Test
    void developerWithoutProfileListsLikeAnonymous() {
        User devUser = TestData.user(2L, "dev@gmail.com", "DEVELOPER");
        when(userRepository.findByEmail("dev@gmail.com")).thenReturn(Optional.of(devUser));
        when(developerProfileRepository.findByUser(devUser)).thenReturn(Optional.empty());
        Pageable page = PageRequest.of(0, 10);

        service.getActiveJobs(null, null, null, page, "dev@gmail.com");

        verify(self).getCachedActiveJobs(null, null, null, page, List.of());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listingWithoutDeveloperSkillsUsesDatabasePaging() {
        JobPosting job = TestData.job(1L, recruiter, JobStatus.ACTIVE, null);
        Pageable page = PageRequest.of(0, 10);
        when(jobPostingRepository.findAll(any(Specification.class), eq(page)))
                .thenReturn(new PageImpl<>(List.of(job), page, 1));

        CustomPageResponse<JobPostingResponse> result = service.getCachedActiveJobs(null, null, null, page, List.of());

        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getId());
        assertFalse(result.getContent().get(0) instanceof JobPostingWithMatchResponse);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listingWithDeveloperSkillsRanksByMatchAndPagesInMemory() {
        JobPosting none = TestData.job(1L, recruiter, JobStatus.ACTIVE, null);
        none.setRequiredSkills(TestData.skills(TestData.skill(9L, "Rust")));
        JobPosting full = TestData.job(2L, recruiter, JobStatus.ACTIVE, null);
        full.setRequiredSkills(TestData.skills(TestData.skill(1L, "Java"), TestData.skill(2L, "SQL")));
        JobPosting half = TestData.job(3L, recruiter, JobStatus.ACTIVE, null);
        half.setRequiredSkills(TestData.skills(TestData.skill(1L, "Java")));
        when(jobPostingRepository.findAll(any(Specification.class))).thenReturn(List.of(none, full, half));

        CustomPageResponse<JobPostingResponse> first =
                service.getCachedActiveJobs(null, null, null, PageRequest.of(0, 2), List.of(1L, 2L));

        assertEquals(3, first.getTotalElements());
        assertEquals(2, first.getTotalPages());
        assertFalse(first.isLast());
        assertEquals(List.of(2L, 3L), first.getContent().stream().map(JobPostingResponse::getId).toList());
        JobPostingWithMatchResponse best = assertInstanceOf(JobPostingWithMatchResponse.class, first.getContent().get(0));
        assertEquals(100.0, best.getMatchPercentage(), 0.001);

        CustomPageResponse<JobPostingResponse> second =
                service.getCachedActiveJobs(null, null, null, PageRequest.of(1, 2), List.of(1L, 2L));
        assertEquals(List.of(1L), second.getContent().stream().map(JobPostingResponse::getId).toList());
        assertTrue(second.isLast());

        CustomPageResponse<JobPostingResponse> beyond =
                service.getCachedActiveJobs(null, null, null, PageRequest.of(5, 2), List.of(1L, 2L));
        assertTrue(beyond.getContent().isEmpty());
    }
}
