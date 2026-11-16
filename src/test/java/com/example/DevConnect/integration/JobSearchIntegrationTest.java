package com.example.DevConnect.integration;

import com.example.DevConnect.dto.response.CustomPageResponse;
import com.example.DevConnect.dto.response.JobPostingResponse;
import com.example.DevConnect.dto.response.JobPostingWithMatchResponse;
import com.example.DevConnect.entity.JobPosting;
import com.example.DevConnect.entity.RecruiterProfile;
import com.example.DevConnect.entity.Skill;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.enums.JobStatus;
import com.example.DevConnect.enums.JobType;
import com.example.DevConnect.repository.JobPostingRepository;
import com.example.DevConnect.repository.RecruiterProfileRepository;
import com.example.DevConnect.repository.SkillRepository;
import com.example.DevConnect.repository.UserRepository;
import com.example.DevConnect.service.JobPostingService;
import com.example.DevConnect.support.IntegrationTest;
import com.example.DevConnect.support.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The listing filters are JPA Specifications, so they are only really tested
 * against MySQL. Every job here sits in a location unique to the run, which keeps
 * existing rows in the database out of the results.
 */
@IntegrationTest
@Transactional
class JobSearchIntegrationTest {

    @Autowired
    private JobPostingService jobPostingService;
    @Autowired
    private JobPostingRepository jobPostingRepository;
    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private CacheManager cacheManager;

    private String town;
    private Skill java;
    private Skill sql;
    private JobPosting remoteJava;
    private JobPosting onsiteSql;
    private JobPosting remoteBoth;

    @BeforeEach
    void seed() {
        clearCache();
        String run = UUID.randomUUID().toString().substring(0, 8);
        town = "Searchville-" + run;

        java = skillRepository.save(new Skill(null, "SearchJava-" + run));
        sql = skillRepository.save(new Skill(null, "SearchSql-" + run));

        User user = TestData.user(null, "search" + run + "@gmail.com", "RECRUITER");
        user.setUserName("search" + run);
        userRepository.save(user);
        RecruiterProfile recruiter = recruiterProfileRepository.save(TestData.recruiter(null, user, "Search Co"));

        remoteJava = save(recruiter, "A Remote Java", JobType.REMOTE, JobStatus.ACTIVE, java);
        onsiteSql = save(recruiter, "B Onsite SQL", JobType.ONSITE, JobStatus.ACTIVE, sql);
        remoteBoth = save(recruiter, "C Remote Both", JobType.REMOTE, JobStatus.ACTIVE, java, sql);
        save(recruiter, "D Closed Java", JobType.REMOTE, JobStatus.CLOSED, java);
    }

    /** invalidate(), not clear(): Redis cache clears are applied asynchronously. */
    @AfterEach
    void clearCache() {
        cacheManager.getCache("job-listings").invalidate();
    }

    private JobPosting save(RecruiterProfile recruiter, String title, JobType type, JobStatus status, Skill... skills) {
        JobPosting job = TestData.job(null, recruiter, status, TestData.daysFromNow(10));
        job.setTitle(title);
        job.setJobType(type);
        job.setLocation(town);
        job.setRequiredSkills(TestData.skills(skills));
        return jobPostingRepository.save(job);
    }

    private List<String> titles(List<String> skills, String location, JobType type) {
        CustomPageResponse<JobPostingResponse> page = jobPostingService.getActiveJobs(
                skills, location, type, PageRequest.of(0, 50, Sort.by("title")), null);
        return page.getContent().stream().map(JobPostingResponse::getTitle).toList();
    }

    @Test
    void onlyActiveJobsAreListed() {
        assertEquals(List.of("A Remote Java", "B Onsite SQL", "C Remote Both"), titles(null, town, null));
    }

    @Test
    void locationMatchesCaseInsensitivelyAndPartially() {
        assertEquals(3, titles(null, "  " + town.toUpperCase().substring(0, town.length() - 2) + " ", null).size());
    }

    @Test
    void jobTypeFilters() {
        assertEquals(List.of("A Remote Java", "C Remote Both"), titles(null, town, JobType.REMOTE));
    }

    @Test
    void skillFilterIsCaseInsensitiveAndDoesNotDuplicateJobs() {
        assertEquals(List.of("A Remote Java", "C Remote Both"), titles(List.of(java.getName().toLowerCase()), town, null));

        // A job matching both requested skills still appears once.
        assertEquals(List.of("A Remote Java", "B Onsite SQL", "C Remote Both"),
                titles(List.of(java.getName(), sql.getName()), town, null));
    }

    @Test
    void blankSkillValuesAreIgnored() {
        assertEquals(3, titles(List.of(" ", ""), town, null).size());
    }

    @Test
    void developerSkillsRankJobsByMatch() {
        CustomPageResponse<JobPostingResponse> page = jobPostingService.getCachedActiveJobs(
                null, town, null, PageRequest.of(0, 50), List.of(java.getId()));

        Map<String, Double> scores = page.getContent().stream()
                .map(r -> (JobPostingWithMatchResponse) r)
                .collect(Collectors.toMap(JobPostingResponse::getTitle, JobPostingWithMatchResponse::getMatchPercentage));
        assertEquals(100.0, scores.get(remoteJava.getTitle()), 0.001);
        assertEquals(50.0, scores.get(remoteBoth.getTitle()), 0.001);
        assertEquals(0.0, scores.get(onsiteSql.getTitle()), 0.001);

        List<String> order = page.getContent().stream().map(JobPostingResponse::getTitle).toList();
        assertTrue(order.indexOf(remoteJava.getTitle()) < order.indexOf(remoteBoth.getTitle()));
        assertTrue(order.indexOf(remoteBoth.getTitle()) < order.indexOf(onsiteSql.getTitle()));
    }
}
