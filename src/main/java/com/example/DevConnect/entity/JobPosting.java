package com.example.DevConnect.entity;

import com.example.DevConnect.enums.JobStatus;
import com.example.DevConnect.enums.JobType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;
import java.util.Set;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "job_postings")
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "recruiter_id", nullable = false)
    private RecruiterProfile recruiter;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private JobType jobType;

    private String location;

    private Integer experienceRequired;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private Date createdAt;

    private Date expiresAt;

    @ManyToMany
    @JoinTable(
            name = "job_required_skills",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> requiredSkills;
}
