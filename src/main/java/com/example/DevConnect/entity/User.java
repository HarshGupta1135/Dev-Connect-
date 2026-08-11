package com.example.DevConnect.entity;

import com.example.DevConnect.enums.EmailPreference;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String userName;

    private String password;

    @Column(unique = true)
    @NonNull
    private String email;

    /**
     * An optional second address for notifications only. Deliberately not unique and
     * never used to look a user up: the primary email is the sign-in identity and the
     * JWT subject, and this must not become a second way to claim an account.
     */
    private String secondaryEmail;

    @Enumerated(EnumType.STRING)
    private EmailPreference emailPreference;

    @CreationTimestamp
    @Column(updatable = false)
    private Date created_at;

    private List<String> role;

    /**
     * The address notifications should go to.
     *
     * Not named getX so it stays out of any JSON serialisation of this entity — it is
     * derived state, not a column. Falls back to the primary address whenever the
     * secondary one is missing, so a stale preference can never produce a null
     * recipient.
     */
    public String resolveNotificationEmail() {
        if (emailPreference == EmailPreference.SECONDARY
                && secondaryEmail != null
                && !secondaryEmail.isBlank()) {
            return secondaryEmail;
        }
        return email;
    }
}
