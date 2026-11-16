package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.RecruiterProfileRequest;
import com.example.DevConnect.dto.response.DeveloperProfileResponse;
import com.example.DevConnect.dto.response.RecruiterProfileResponse;
import com.example.DevConnect.entity.DeveloperProfile;
import com.example.DevConnect.entity.RecruiterProfile;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.repository.DeveloperProfileRepository;
import com.example.DevConnect.repository.RecruiterProfileRepository;
import com.example.DevConnect.repository.UserRepository;
import com.example.DevConnect.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecruiterProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RecruiterProfileRepository recruiterProfileRepository;
    @Mock
    private DeveloperProfileRepository developerProfileRepository;

    @InjectMocks
    private RecruiterProfileService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = TestData.user(1L, "rec@gmail.com", "RECRUITER");
        when(userRepository.findByEmail("rec@gmail.com")).thenReturn(Optional.of(user));
    }

    @Test
    void createProfileMapsDescriptionToCompanyDesc() {
        when(recruiterProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        String message = service.createProfile(
                new RecruiterProfileRequest("Rec Ruiter", "Acme", "We build", "https://acme.dev", "Delhi"), "rec@gmail.com");

        ArgumentCaptor<RecruiterProfile> saved = ArgumentCaptor.forClass(RecruiterProfile.class);
        verify(recruiterProfileRepository).save(saved.capture());
        assertEquals("Profile Created Successfully", message);
        assertEquals("Acme", saved.getValue().getCompanyName());
        assertEquals("We build", saved.getValue().getCompanyDesc());
        assertEquals(user, saved.getValue().getUser());
    }

    @Test
    void createProfileTwiceIsRejected() {
        when(recruiterProfileRepository.findByUser(user)).thenReturn(Optional.of(new RecruiterProfile()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createProfile(
                new RecruiterProfileRequest("Rec", null, null, null, null), "rec@gmail.com"));
        assertEquals("Profile already exists for this user", ex.getMessage());
    }

    @Test
    void getProfileMapsCompanyDescBackToDescription() {
        when(recruiterProfileRepository.findByUser(user)).thenReturn(Optional.of(TestData.recruiter(3L, user, "Acme")));

        RecruiterProfileResponse response = service.getProfile("rec@gmail.com");

        assertEquals(3L, response.getId());
        assertEquals("Acme", response.getCompanyName());
        assertEquals("Hiring", response.getDescription());
    }

    @Test
    void getProfileWithoutProfileFails() {
        when(recruiterProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.getProfile("rec@gmail.com"));
    }

    @Test
    void updateProfileIgnoresBlankFields() {
        RecruiterProfile existing = TestData.recruiter(3L, user, "Acme");
        when(recruiterProfileRepository.findByUser(user)).thenReturn(Optional.of(existing));

        service.updateProfile(new RecruiterProfileRequest("New Name", " ", null, "", "Chennai"), "rec@gmail.com");

        assertEquals("New Name", existing.getFullName());
        assertEquals("Acme", existing.getCompanyName());
        assertEquals("Hiring", existing.getCompanyDesc());
        assertEquals("https://example.com", existing.getWebsite());
        assertEquals("Chennai", existing.getLocation());
        verify(recruiterProfileRepository).save(existing);
    }

    @Test
    void getDeveloperProfileReturnsPublicFieldsOnly() {
        DeveloperProfile dev = TestData.completeDeveloper(7L, TestData.user(2L, "dev@gmail.com", "DEVELOPER"));
        dev.setSkills(TestData.skills(TestData.skill(1L, "Java")));
        when(developerProfileRepository.findById(7L)).thenReturn(Optional.of(dev));

        DeveloperProfileResponse response = service.getDeveloperProfile("rec@gmail.com", 7L);

        assertEquals("Dev Eloper", response.getFullName());
        assertEquals(List.of("Java"), response.getSkills());
        // Contact details are not part of this lookup.
        assertNull(response.getPhone());
        assertNull(response.getAddress());
    }

    @Test
    void getDeveloperProfileForUnknownIdFails() {
        when(developerProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.getDeveloperProfile("rec@gmail.com", 99L));
    }
}
