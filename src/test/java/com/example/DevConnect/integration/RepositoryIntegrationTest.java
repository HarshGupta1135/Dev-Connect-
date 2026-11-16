package com.example.DevConnect.integration;

import com.example.DevConnect.entity.Application;
import com.example.DevConnect.entity.DeveloperProfile;
import com.example.DevConnect.entity.JobPosting;
import com.example.DevConnect.entity.RecruiterProfile;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.enums.ApplicationStatus;
import com.example.DevConnect.enums.JobStatus;
import com.example.DevConnect.repository.ApplicationRepository;
import com.example.DevConnect.repository.DeveloperProfileRepository;
import com.example.DevConnect.repository.JobPostingRepository;
import com.example.DevConnect.repository.RecruiterProfileRepository;
import com.example.DevConnect.repository.UserRepository;
import com.example.DevConnect.support.IntegrationTest;
import com.example.DevConnect.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The custom and derived queries the services and schedulers rely on, against MySQL. */
@IntegrationTest
@Transactional
class RepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DeveloperProfileRepository developerProfileRepository;
    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;
    @Autowired
    private JobPostingRepository jobPostingRepository;
    @Autowired
    private ApplicationRepository applicationRepository;

    private String run;
    private RecruiterProfile recruiter;
    private JobPosting job;

    @BeforeEach
    void seed() {
        run = UUID.randomUUID().toString().substring(0, 8);
        recruiter = recruiterProfileRepository.save(TestData.recruiter(null, saveUser("reporec", "RECRUITER"), "Repo Co"));
        job = jobPostingRepository.save(TestData.job(null, recruiter, JobStatus.ACTIVE, TestData.daysFromNow(5)));
    }

    private User saveUser(String prefix, String role) {
        User user = TestData.user(null, prefix + run + "@gmail.com", role);
        user.setUserName(prefix + run);
        return userRepository.save(user);
    }

    private DeveloperProfile saveDeveloper(String prefix) {
        return developerProfileRepository.save(TestData.completeDeveloper(null, saveUser(prefix, "DEVELOPER")));
    }

    private Application apply(DeveloperProfile dev, ApplicationStatus status, boolean mailSent) {
        Application app = TestData.application(null, dev, job, status);
        app.setMailSent(mailSent);
        return applicationRepository.save(app);
    }

    @Test
    void usersAreFoundByEmailAndUsername() {
        User user = saveUser("repouser", "DEVELOPER");

        assertEquals(user.getId(), userRepository.findByEmail("repouser" + run + "@gmail.com").orElseThrow().getId());
        assertEquals(user.getId(), userRepository.findByUserName("repouser" + run).getId());
        assertNotNull(user.getCreated_at(), "creation timestamp is set by Hibernate");
    }

    @Test
    void rolesRoundTripThroughTheJsonColumn() {
        User user = saveUser("reporoles", "ADMIN");
        user.setRole(List.of("ADMIN", "USER"));
        userRepository.saveAndFlush(user);

        assertEquals(List.of("ADMIN", "USER"), userRepository.findById(user.getId()).orElseThrow().getRole());
    }

    @Test
    void duplicateApplicationsAreDetected() {
        DeveloperProfile dev = saveDeveloper("repodup");
        assertFalse(applicationRepository.existsByDeveloperAndJob(dev, job));

        apply(dev, ApplicationStatus.APPLIED, false);

        assertTrue(applicationRepository.existsByDeveloperAndJob(dev, job));
    }

    @Test
    void pendingStatusMailsAreFoundByStatusAndFlag() {
        Application unsent = apply(saveDeveloper("repounsent"), ApplicationStatus.REJECTED, false);
        Application sent = apply(saveDeveloper("reposent"), ApplicationStatus.REJECTED, true);

        List<Long> pending = applicationRepository.findByStatusAndMailSent(ApplicationStatus.REJECTED, false)
                .stream().map(Application::getId).toList();

        assertTrue(pending.contains(unsent.getId()));
        assertFalse(pending.contains(sent.getId()));
    }

    @Test
    void rejectedAndShortlistedDeveloperQueriesReturnTheirUsers() {
        DeveloperProfile rejected = saveDeveloper("reporej");
        DeveloperProfile shortlisted = saveDeveloper("reposhort");
        apply(rejected, ApplicationStatus.REJECTED, false);
        apply(shortlisted, ApplicationStatus.SHORTLISTED, false);

        List<String> rejectedEmails = applicationRepository.getRejectedDevelopers().stream().map(User::getEmail).toList();
        List<String> shortlistedEmails = applicationRepository.getShortlistedDevelopers().stream().map(User::getEmail).toList();

        assertTrue(rejectedEmails.contains(rejected.getUser().getEmail()));
        assertFalse(rejectedEmails.contains(shortlisted.getUser().getEmail()));
        assertTrue(shortlistedEmails.contains(shortlisted.getUser().getEmail()));
    }

    @Test
    void applicationsAreListedPerDeveloperAndPerJob() {
        DeveloperProfile dev = saveDeveloper("repolist");
        Application app = apply(dev, ApplicationStatus.APPLIED, false);

        assertEquals(List.of(app.getId()), applicationRepository.findByDeveloper(dev).stream().map(Application::getId).toList());
        assertTrue(applicationRepository.findByJob(job).stream().anyMatch(a -> a.getId().equals(app.getId())));
    }

    @Test
    void expiredActiveJobsAreFoundForTheScheduler() {
        JobPosting expired = jobPostingRepository.save(TestData.job(null, recruiter, JobStatus.ACTIVE, TestData.daysFromNow(-1)));
        JobPosting expiredButClosed = jobPostingRepository.save(TestData.job(null, recruiter, JobStatus.CLOSED, TestData.daysFromNow(-1)));

        List<Long> ids = jobPostingRepository.findByStatusAndExpiresAtBefore(JobStatus.ACTIVE, new Date())
                .stream().map(JobPosting::getId).toList();

        assertTrue(ids.contains(expired.getId()));
        assertFalse(ids.contains(job.getId()), "not yet expired");
        assertFalse(ids.contains(expiredButClosed.getId()), "already closed");
    }

    @Test
    void recruiterJobsAreListed() {
        assertEquals(List.of(job.getId()),
                jobPostingRepository.findByRecruiter(recruiter).stream().map(JobPosting::getId).toList());
    }
}
