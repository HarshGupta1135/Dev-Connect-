package com.example.DevConnect.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "developer_profiles")
public class DeveloperProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    private String fullName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String location;

    private Integer yearsExp;

    private String resumeUrl;

    /* Contact details required before an application is accepted — see
       ApplicationService.requireCompleteProfile. Added by V2. */
    private String phone;

    private String address;

    private String city;

    private String pincode;

    private String linkedinUrl;

    @ManyToMany
    @JoinTable(
            name = "developer_skills",
            joinColumns = @JoinColumn(name = "developer_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> skills;
}
