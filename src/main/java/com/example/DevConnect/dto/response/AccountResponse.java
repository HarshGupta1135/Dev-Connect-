package com.example.DevConnect.dto.response;

import com.example.DevConnect.enums.EmailPreference;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountResponse {
    private Long id;
    private String userName;
    private String email;
    private String secondaryEmail;
    private EmailPreference emailPreference;

    /**
     * Where notifications will actually be delivered, resolved server-side so the
     * client never has to reimplement the fallback rule.
     */
    private String notificationEmail;

    private List<String> role;
    private Date createdAt;

    /**
     * A replacement JWT, set only when the primary email changed. The token's subject
     * is that address, so the one the caller just authenticated with stops resolving to
     * a user the moment it changes — without this the client would be signed out.
     */
    private String token;
}
