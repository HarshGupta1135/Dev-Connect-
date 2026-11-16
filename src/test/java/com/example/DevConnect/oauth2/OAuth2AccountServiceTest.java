package com.example.DevConnect.oauth2;

import com.example.DevConnect.entity.User;
import com.example.DevConnect.oauth2.OAuth2AccountService.OAuth2SignInException;
import com.example.DevConnect.repository.UserRepository;
import com.example.DevConnect.service.EmailService;
import com.example.DevConnect.support.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2AccountServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private OAuth2AccountService service;

    private User created() {
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void existingAccountWithThatEmailIsReturnedUnchanged() {
        User existing = TestData.user(1L, "harsh@gmail.com", "RECRUITER");
        when(userRepository.findByEmail("harsh@gmail.com")).thenReturn(Optional.of(existing));

        User user = service.findOrCreate(" harsh@gmail.com ", "Harsh", "DEVELOPER");

        assertSame(existing, user);
        assertEquals(List.of("RECRUITER"), user.getRole(), "an existing account keeps its role");
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendWelcomeEmail(any(), any());
    }

    @Test
    void newAccountWithoutAChosenRoleIsRefused() {
        when(userRepository.findByEmail("new@gmail.com")).thenReturn(Optional.empty());

        OAuth2SignInException ex = assertThrows(OAuth2SignInException.class,
                () -> service.findOrCreate("new@gmail.com", "New", null));

        assertEquals("role_required", ex.getCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void newAccountIsCreatedWithRoleNoPasswordAndAWelcomeEmail() {
        when(userRepository.findByEmail("new@outlook.com")).thenReturn(Optional.empty());

        service.findOrCreate("new@outlook.com", "Harsh Gupta", "RECRUITER");

        User user = created();
        assertEquals("new@outlook.com", user.getEmail());
        assertEquals("Harsh_Gupta", user.getUserName());
        assertEquals(List.of("RECRUITER"), user.getRole());
        assertNull(user.getPassword(), "provider accounts have no password");
        verify(emailService).sendWelcomeEmail("new@outlook.com", "Harsh_Gupta");
    }

    @Test
    void takenUsernameGetsANumericSuffix() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUserName("octocat")).thenReturn(new User());
        when(userRepository.findByUserName("octocat1")).thenReturn(new User());

        service.findOrCreate("octo@gmail.com", "octocat", "DEVELOPER");

        assertEquals("octocat2", created().getUserName());
    }

    @Test
    void missingNameFallsBackToTheEmailLocalPart() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        service.findOrCreate("jane.doe+jobs@gmail.com", null, "DEVELOPER");

        assertEquals("jane.doejobs", created().getUserName());
    }

    @Test
    void nameMadeOnlyOfSymbolsFallsBackToTheEmail() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        service.findOrCreate("jane@gmail.com", "🙂🙂", "DEVELOPER");

        assertEquals("jane", created().getUserName());
    }

    @Test
    void longNamesAreCutToThirtyCharactersEvenWithASuffix() {
        String longName = "a".repeat(40);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUserName("a".repeat(30))).thenReturn(new User());

        service.findOrCreate("long@gmail.com", longName, "DEVELOPER");

        String userName = created().getUserName();
        assertEquals("a".repeat(29) + "1", userName);
        assertTrue(userName.length() <= 30);
    }
}
