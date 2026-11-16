package com.example.DevConnect.integration;

import com.example.DevConnect.entity.Skill;
import com.example.DevConnect.repository.SkillRepository;
import com.example.DevConnect.service.CloudinaryService;
import com.example.DevConnect.service.EmailService;
import com.example.DevConnect.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole hiring flow through the real HTTP API, security, services and MySQL:
 * register, sign in, build profiles, post a job, apply, and shortlist.
 *
 * Email and Cloudinary are replaced, so nothing leaves the machine. Everything
 * runs in one transaction that is rolled back, and names are unique per run,
 * so it is safe against a database that already has data in it.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class ApiFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private EmailService emailService;
    @MockitoBean
    private CloudinaryService cloudinaryService;

    private String run;
    private String skillName;

    @BeforeEach
    void setUp() {
        run = UUID.randomUUID().toString().substring(0, 8);
        skillName = "FlowSkill-" + run;
        skillRepository.save(new Skill(null, skillName));
        clearListingCache();
    }

    /**
     * Listings cached during the test describe rows that are about to be rolled back.
     * invalidate(), not clear(): Redis cache clears are applied asynchronously.
     */
    @AfterEach
    void clearListingCache() {
        cacheManager.getCache("job-listings").invalidate();
    }

    private void register(String user, String role) throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json").content("""
                        {"username":"%s","email":"%s@gmail.com","password":"Secret@123","role":"%s"}
                        """.formatted(user, user, role)))
                .andExpect(status().isOk());
    }

    private String login(String user) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType("application/json").content("""
                        {"email":"%s@gmail.com","password":"Secret@123"}
                        """.formatted(user)))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    @Test
    void developerAppliesAndRecruiterShortlists() throws Exception {
        String recruiter = "flowrec" + run;
        String developer = "flowdev" + run;

        // --- accounts ---
        register(recruiter, "RECRUITER");
        register(developer, "DEVELOPER");
        verify(emailService).sendWelcomeEmail(recruiter + "@gmail.com", recruiter);
        String rec = login(recruiter);
        String dev = login(developer);

        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"email\":\"" + developer + "@gmail.com\",\"password\":\"Wrong@123\"}"))
                .andExpect(status().isUnauthorized());

        // --- recruiter posts a job ---
        mvc.perform(post("/api/recruiter/profile").header("Authorization", rec).contentType("application/json")
                        .content("{\"fullName\":\"Flow Recruiter\",\"companyName\":\"Flow Co " + run + "\"}"))
                .andExpect(status().isOk());
        String jobTitle = "Flow Engineer " + run;
        mvc.perform(post("/api/recruiter/jobs").header("Authorization", rec).contentType("application/json").content("""
                        {"title":"%s","description":"d","jobType":"REMOTE","location":"Flowtown %s",
                         "experienceRequired":1,"status":"ACTIVE","requiredSkills":["%s"]}
                        """.formatted(jobTitle, run, skillName)))
                .andExpect(status().isOk());
        MvcResult jobs = mvc.perform(get("/api/recruiter/jobs").header("Authorization", rec))
                .andExpect(status().isOk())
                .andReturn();
        Integer jobId = JsonPath.read(jobs.getResponse().getContentAsString(), "$.data[0].id");

        // --- developer cannot apply before the profile is complete ---
        mvc.perform(post("/api/developer/profile").header("Authorization", dev).contentType("application/json").content("""
                        {"fullName":"Flow Dev","location":"Flowtown","phone":"9999999999","address":"1 Road",
                         "city":"Flowtown","pincode":"411001","skills":["%s"]}
                        """.formatted(skillName)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/developer/apply").header("Authorization", dev).contentType("application/json")
                        .content("{\"jobId\":" + jobId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Complete your profile before applying. Missing: resume"));

        when(cloudinaryService.uploadResume(any())).thenReturn("https://cdn.example/" + run + ".pdf");
        mvc.perform(multipart("/api/developer/profile/resume").header("Authorization", dev)
                        .file(new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[]{1})))
                .andExpect(status().isOk());

        // --- the job shows up for the developer, scored against their skills ---
        mvc.perform(get("/api/jobs").header("Authorization", dev).param("location", "Flowtown " + run))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value(jobTitle))
                .andExpect(jsonPath("$.data.content[0].matchPercentage").value(100.0));

        // --- apply, once ---
        mvc.perform(post("/api/developer/apply").header("Authorization", dev).contentType("application/json")
                        .content("{\"jobId\":" + jobId + ",\"coverNote\":\"Hello\"}"))
                .andExpect(status().isOk());
        verify(emailService).sendApplicationConfirmationEmail(
                eq(developer + "@gmail.com"), eq("Flow Dev"), eq(jobTitle), eq("Flow Co " + run));
        mvc.perform(post("/api/developer/apply").header("Authorization", dev).contentType("application/json")
                        .content("{\"jobId\":" + jobId + "}"))
                .andExpect(status().isConflict());

        // --- recruiter reviews and shortlists ---
        MvcResult applicants = mvc.perform(get("/api/recruiter/jobs/" + jobId + "/applications").header("Authorization", rec))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].applicant.email").value(developer + "@gmail.com"))
                .andExpect(jsonPath("$.data[0].applicant.skills", hasItem(skillName)))
                .andReturn();
        Integer applicationId = JsonPath.read(applicants.getResponse().getContentAsString(), "$.data[0].id");

        mvc.perform(patch("/api/recruiter/applications/" + applicationId + "/status").header("Authorization", dev)
                        .contentType("application/json").content("{\"newStatus\":\"SHORTLISTED\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/recruiter/applications/" + applicationId + "/status").header("Authorization", rec)
                        .contentType("application/json").content("{\"newStatus\":\"SHORTLISTED\"}"))
                .andExpect(status().isOk());
        verify(emailService).sendStatusUpdateEmail(developer + "@gmail.com", "Flow Dev", jobTitle, "SHORTLISTED");

        mvc.perform(get("/api/developer/applications").header("Authorization", dev))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("SHORTLISTED"));

        // --- closing the job takes it off the public board ---
        mvc.perform(patch("/api/recruiter/jobs/" + jobId + "/close").header("Authorization", rec))
                .andExpect(status().isOk());
        mvc.perform(get("/api/jobs").param("location", "Flowtown " + run))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void duplicateRegistrationIsRefused() throws Exception {
        String user = "flowdup" + run;
        register(user, "DEVELOPER");

        mvc.perform(post("/api/auth/register").contentType("application/json").content("""
                        {"username":"%s","email":"other%s@gmail.com","password":"Secret@123","role":"DEVELOPER"}
                        """.formatted(user, run)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void accountEmailChangeIssuesATokenThatWorks() throws Exception {
        String user = "flowacct" + run;
        register(user, "DEVELOPER");
        String token = login(user);

        MvcResult updated = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/account")
                        .header("Authorization", token).contentType("application/json")
                        .content("{\"userName\":\"" + user + "\",\"email\":\"" + user + "new@gmail.com\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String newToken = JsonPath.read(updated.getResponse().getContentAsString(), "$.data.token");

        // The old token names the old email, which no longer exists.
        mvc.perform(get("/api/account/me").header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/account/me").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(user + "new@gmail.com"))
                .andExpect(jsonPath("$.data.role[0]").value("DEVELOPER"));
    }
}
