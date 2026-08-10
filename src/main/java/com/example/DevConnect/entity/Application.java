package com.example.DevConnect.entity;

import com.example.DevConnect.enums.ApplicationStatus;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(
        name = "applications",
        // Last line of defence against double applications: two concurrent requests can both
        // pass the exists-check before either has committed.
        uniqueConstraints = @UniqueConstraint(
                name = "uk_application_developer_job",
                columnNames = {"developer_id", "job_id"}
        ),
        indexes = {
                // Used by the status-mail retry scheduler.
                @Index(name = "idx_application_status_mail_sent", columnList = "status, mail_sent"),
                @Index(name = "idx_application_job", columnList = "job_id")
        }
)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "developer_id", nullable = false)
    private DeveloperProfile developer;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private JobPosting job;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Builder.Default
    private boolean mailSent = false;

    @Column(columnDefinition = "TEXT")
    private String coverNote;

    @CreationTimestamp
    @Column(updatable = false)
    private Date appliedAt;

    @UpdateTimestamp
    private Date updatedAt;
}
