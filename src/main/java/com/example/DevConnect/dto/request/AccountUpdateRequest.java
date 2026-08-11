package com.example.DevConnect.dto.request;

import com.example.DevConnect.enums.EmailPreference;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login details on the user record, edited from either role's profile page.
 * Password changes are deliberately not part of this — they need the current
 * password as well, which is a separate flow.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountUpdateRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 40, message = "Username must be between 3 and 40 characters")
    private String userName;

    @NotBlank(message = "Email is required")
    @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Invalid email format"
    )
    private String email;

    /**
     * Optional. Blank or null clears it — and clears a SECONDARY preference with it,
     * since there would be nowhere to deliver to. Any provider is accepted: unlike the
     * primary address this is only a delivery target, never a sign-in identity.
     */
    @Pattern(
            regexp = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Invalid secondary email format"
    )
    private String secondaryEmail;

    /** Null leaves the current choice untouched. */
    private EmailPreference emailPreference;
}
