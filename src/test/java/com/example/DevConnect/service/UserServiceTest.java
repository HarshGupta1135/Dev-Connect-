package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.AccountUpdateRequest;
import com.example.DevConnect.dto.response.AccountResponse;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.enums.EmailPreference;
import com.example.DevConnect.exception.BadRequestException;
import com.example.DevConnect.exception.ConflictException;
import com.example.DevConnect.exception.ResourceNotFoundException;
import com.example.DevConnect.repository.UserRepository;
import com.example.DevConnect.support.TestData;
import com.example.DevConnect.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = TestData.user(1L, "harsh@gmail.com", "DEVELOPER");
        when(userRepository.findByEmail("harsh@gmail.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static AccountUpdateRequest update(String userName, String email, String secondary, EmailPreference pref) {
        return new AccountUpdateRequest(userName, email, secondary, pref);
    }

    @Test
    void getAccountReportsPrimaryPreferenceWhenColumnIsNull() {
        user.setEmailPreference(null);

        AccountResponse account = userService.getAccount("harsh@gmail.com");

        assertEquals("harsh@gmail.com", account.getEmail());
        assertEquals(EmailPreference.PRIMARY, account.getEmailPreference());
        assertEquals("harsh@gmail.com", account.getNotificationEmail());
        assertNull(account.getToken());
    }

    @Test
    void getAccountForUnknownUserIsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> userService.getAccount("ghost@gmail.com"));
    }

    @Test
    void unchangedDetailsDoNotIssueANewToken() {
        AccountResponse account = userService.updateAccount("harsh@gmail.com",
                update("harsh", "harsh@gmail.com", null, null));

        assertNull(account.getToken());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void renamingToAFreeUsernameIsSaved() {
        when(userRepository.findByUserName("harsh_new")).thenReturn(null);

        AccountResponse account = userService.updateAccount("harsh@gmail.com",
                update("  harsh_new ", "harsh@gmail.com", null, null));

        assertEquals("harsh_new", account.getUserName());
    }

    @Test
    void renamingToSomeoneElsesUsernameConflicts() {
        when(userRepository.findByUserName("taken")).thenReturn(TestData.user(2L, "taken@gmail.com", "DEVELOPER"));

        assertThrows(ConflictException.class, () -> userService.updateAccount("harsh@gmail.com",
                update("taken", "harsh@gmail.com", null, null)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changingEmailIssuesAReplacementToken() {
        when(userRepository.findByEmail("new@gmail.com")).thenReturn(Optional.empty());
        when(jwtUtil.generateToken(any())).thenReturn("new-token");

        AccountResponse account = userService.updateAccount("harsh@gmail.com",
                update("harsh", "new@gmail.com", null, null));

        assertEquals("new@gmail.com", account.getEmail());
        assertEquals("new-token", account.getToken());
    }

    @Test
    void changingEmailToNonGmailIsRejected() {
        assertThrows(BadRequestException.class, () -> userService.updateAccount("harsh@gmail.com",
                update("harsh", "new@yahoo.com", null, null)));
    }

    @Test
    void changingEmailToAnotherAccountsAddressConflicts() {
        when(userRepository.findByEmail("other@gmail.com"))
                .thenReturn(Optional.of(TestData.user(2L, "other@gmail.com", "DEVELOPER")));

        assertThrows(ConflictException.class, () -> userService.updateAccount("harsh@gmail.com",
                update("harsh", "other@gmail.com", null, null)));
    }

    @Test
    void secondaryEmailIsTrimmedAndStored() {
        AccountResponse account = userService.updateAccount("harsh@gmail.com",
                update("harsh", "harsh@gmail.com", "  alt@example.com ", null));

        assertEquals("alt@example.com", account.getSecondaryEmail());
    }

    @Test
    void secondaryEmailMatchingPrimaryIsRejected() {
        assertThrows(BadRequestException.class, () -> userService.updateAccount("harsh@gmail.com",
                update("harsh", "harsh@gmail.com", "HARSH@gmail.com", null)));
    }

    @Test
    void clearingSecondaryEmailFallsBackToPrimaryPreference() {
        user.setSecondaryEmail("alt@example.com");
        user.setEmailPreference(EmailPreference.SECONDARY);

        AccountResponse account = userService.updateAccount("harsh@gmail.com",
                update("harsh", "harsh@gmail.com", "   ", null));

        assertNull(account.getSecondaryEmail());
        assertEquals(EmailPreference.PRIMARY, account.getEmailPreference());
        assertEquals("harsh@gmail.com", account.getNotificationEmail());
    }

    @Test
    void choosingSecondaryWithoutASecondaryEmailIsRejected() {
        assertThrows(BadRequestException.class, () -> userService.updateAccount("harsh@gmail.com",
                update("harsh", "harsh@gmail.com", null, EmailPreference.SECONDARY)));
    }

    @Test
    void choosingSecondaryRoutesNotificationsThere() {
        AccountResponse account = userService.updateAccount("harsh@gmail.com",
                update("harsh", "harsh@gmail.com", "alt@example.com", EmailPreference.SECONDARY));

        assertEquals(EmailPreference.SECONDARY, account.getEmailPreference());
        assertEquals("alt@example.com", account.getNotificationEmail());
    }
}
