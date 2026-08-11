package com.example.DevConnect.dto.response;

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
    private List<String> role;
    private Date createdAt;

    /**
     * A replacement JWT, set only when the email changed. The token's subject is the
     * email, so the one the caller just authenticated with stops resolving to a user
     * the moment the address changes — without this the client would be signed out.
     */
    private String token;
}
