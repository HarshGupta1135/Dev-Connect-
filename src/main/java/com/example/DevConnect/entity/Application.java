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
@Table(name = "applications")
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

    @Column(columnDefinition = "TEXT")
    private String coverNote;

    @CreationTimestamp
    @Column(updatable = false)
    private Date appliedAt;

    @UpdateTimestamp
    private Date updatedAt;
}
