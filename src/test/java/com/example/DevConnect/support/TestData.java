package com.example.DevConnect.support;

import com.example.DevConnect.entity.Application;
import com.example.DevConnect.entity.DeveloperProfile;
import com.example.DevConnect.entity.JobPosting;
import com.example.DevConnect.entity.RecruiterProfile;
import com.example.DevConnect.entity.Skill;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.enums.ApplicationStatus;
import com.example.DevConnect.enums.JobStatus;
import com.example.DevConnect.enums.JobType;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Small builders for entities, so each test states only what it is about. */
public final class TestData {

    /** Signing key for tests that create or read JWTs. */
    public static final String JWT_SECRET = "test-secret-that-is-at-least-32-characters-long";

    private TestData() {
    }

    public static User user(Long id, String email, String... roles) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUserName(email.substring(0, email.indexOf('@')));
        user.setPassword("$2a$10$hash");
        user.setRole(List.of(roles));
        return user;
    }

    public static Skill skill(Long id, String name) {
        return new Skill(id, name);
    }

    public static Set<Skill> skills(Skill... skills) {
        return new HashSet<>(Arrays.asList(skills));
    }

    /** A developer profile with every field the apply gate checks filled in. */
    public static DeveloperProfile completeDeveloper(Long id, User user) {
        return DeveloperProfile.builder()
                .id(id)
                .user(user)
                .fullName("Dev Eloper")
                .bio("Builds things")
                .location("Pune")
                .yearsExp(3)
                .resumeUrl("https://cdn.example/resume.pdf")
                .phone("9999999999")
                .address("1 Main Road")
                .city("Pune")
                .pincode("411001")
                .linkedinUrl("https://linkedin.com/in/dev")
                .skills(new HashSet<>())
                .build();
    }

    public static RecruiterProfile recruiter(Long id, User user, String company) {
        return RecruiterProfile.builder()
                .id(id)
                .user(user)
                .fullName("Rec Ruiter")
                .companyName(company)
                .companyDesc("Hiring")
                .website("https://example.com")
                .location("Bengaluru")
                .build();
    }

    public static JobPosting job(Long id, RecruiterProfile recruiter, JobStatus status, Date expiresAt) {
        return JobPosting.builder()
                .id(id)
                .recruiter(recruiter)
                .title("Backend Engineer " + id)
                .description("Spring Boot")
                .jobType(JobType.REMOTE)
                .location("Remote")
                .experienceRequired(2)
                .status(status)
                .expiresAt(expiresAt)
                .requiredSkills(new HashSet<>())
                .build();
    }

    public static Application application(Long id, DeveloperProfile developer, JobPosting job, ApplicationStatus status) {
        return Application.builder()
                .id(id)
                .developer(developer)
                .job(job)
                .status(status)
                .coverNote("Hello")
                .build();
    }

    public static Date daysFromNow(int days) {
        return new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days));
    }
}
