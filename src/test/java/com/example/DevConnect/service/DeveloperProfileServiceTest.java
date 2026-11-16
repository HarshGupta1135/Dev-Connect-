package com.example.DevConnect.service;

import com.example.DevConnect.dto.request.DeveloperProfileRequest;
import com.example.DevConnect.dto.response.DeveloperProfileResponse;
import com.example.DevConnect.entity.DeveloperProfile;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.exception.ResourceNotFoundException;
import com.example.DevConnect.repository.DeveloperProfileRepository;
import com.example.DevConnect.repository.SkillRepository;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeveloperProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DeveloperProfileRepository developerProfileRepository;
    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private DeveloperProfileService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = TestData.user(1L, "dev@gmail.com", "DEVELOPER");
        when(userRepository.findByEmail("dev@gmail.com")).thenReturn(Optional.of(user));
        when(skillRepository.findByName("Java")).thenReturn(Optional.of(TestData.skill(1L, "Java")));
        when(skillRepository.findByName("SQL")).thenReturn(Optional.of(TestData.skill(2L, "SQL")));
    }

    private static DeveloperProfileRequest request(List<String> skills) {
        DeveloperProfileRequest r = new DeveloperProfileRequest();
        r.setFullName("Dev Eloper");
        r.setBio("Bio");
        r.setLocation("Pune");
        r.setYearsExp(4);
        r.setPhone("9999999999");
        r.setAddress("1 Main Road");
        r.setCity("Pune");
        r.setPincode("411001");
        r.setLinkedinUrl("https://linkedin.com/in/dev");
        r.setSkills(skills);
        return r;
    }

    @Test
    void createProfileSavesEveryFieldAndResolvesSkills() {
        when(developerProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        String message = service.createProfile(request(List.of("Java", "SQL")), "dev@gmail.com");

        ArgumentCaptor<DeveloperProfile> saved = ArgumentCaptor.forClass(DeveloperProfile.class);
        verify(developerProfileRepository).save(saved.capture());
        DeveloperProfile p = saved.getValue();
        assertEquals("Profile Saved Successfully", message);
        assertEquals(user, p.getUser());
        assertEquals("Dev Eloper", p.getFullName());
        assertEquals("9999999999", p.getPhone());
        assertEquals("411001", p.getPincode());
        assertEquals(4, p.getYearsExp());
        assertEquals(List.of("Java", "SQL"),
                p.getSkills().stream().map(s -> s.getName()).sorted().collect(Collectors.toList()));
    }

    @Test
    void createProfileTwiceIsRejected() {
        when(developerProfileRepository.findByUser(user)).thenReturn(Optional.of(new DeveloperProfile()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.createProfile(request(null), "dev@gmail.com"));
        assertEquals("Profile already exists for this user", ex.getMessage());
    }

    @Test
    void createProfileWithUnknownSkillIsRejected() {
        when(developerProfileRepository.findByUser(user)).thenReturn(Optional.empty());
        when(skillRepository.findByName("Cobol")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.createProfile(request(List.of("Cobol")), "dev@gmail.com"));
        assertTrue(ex.getMessage().contains("Skill not found: Cobol"));
        verify(developerProfileRepository, never()).save(any());
    }

    @Test
    void createProfileForUnknownUserFails() {
        assertThrows(RuntimeException.class, () -> service.createProfile(request(null), "ghost@gmail.com"));
    }

    @Test
    void updateProfileOnlyChangesProvidedFields() {
        DeveloperProfile existing = TestData.completeDeveloper(5L, user);
        when(developerProfileRepository.findByUser(user)).thenReturn(Optional.of(existing));

        DeveloperProfileRequest partial = new DeveloperProfileRequest();
        partial.setCity("Mumbai");
        partial.setFullName("   "); // blank name is ignored, not wiped

        service.updateProfile(partial, "dev@gmail.com");

        assertEquals("Mumbai", existing.getCity());
        assertEquals("Dev Eloper", existing.getFullName());
        assertEquals("Pune", existing.getLocation());
        assertEquals("9999999999", existing.getPhone());
        verify(developerProfileRepository).save(existing);
    }

    @Test
    void updateProfileReplacesSkillsWhenGiven() {
        DeveloperProfile existing = TestData.completeDeveloper(5L, user);
        existing.setSkills(TestData.skills(TestData.skill(1L, "Java")));
        when(developerProfileRepository.findByUser(user)).thenReturn(Optional.of(existing));

        DeveloperProfileRequest r = new DeveloperProfileRequest();
        r.setSkills(List.of("SQL"));
        service.updateProfile(r, "dev@gmail.com");

        assertEquals(List.of("SQL"), existing.getSkills().stream().map(s -> s.getName()).toList());
    }

    @Test
    void updateProfileWithoutProfileFails() {
        when(developerProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.updateProfile(request(null), "dev@gmail.com"));
        assertEquals("Developer Profile Not Found", ex.getMessage());
    }

    @Test
    void getProfileMapsEveryField() {
        DeveloperProfile existing = TestData.completeDeveloper(5L, user);
        existing.setSkills(TestData.skills(TestData.skill(1L, "Java")));
        when(developerProfileRepository.findByUser(user)).thenReturn(Optional.of(existing));

        DeveloperProfileResponse response = service.getProfile("dev@gmail.com");

        assertEquals(5L, response.getId());
        assertEquals("Dev Eloper", response.getFullName());
        assertEquals("https://cdn.example/resume.pdf", response.getResumeUrl());
        assertEquals("1 Main Road", response.getAddress());
        assertEquals(List.of("Java"), response.getSkills());
    }

    @Test
    void getProfileWithoutProfileIsNotFound() {
        when(developerProfileRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getProfile("dev@gmail.com"));
    }

    @Test
    void updateResumeUrlStoresTheUrl() {
        DeveloperProfile existing = TestData.completeDeveloper(5L, user);
        when(developerProfileRepository.findByUser(user)).thenReturn(Optional.of(existing));

        service.updateResumeUrl("dev@gmail.com", "https://cdn.example/new.pdf");

        assertEquals("https://cdn.example/new.pdf", existing.getResumeUrl());
        verify(developerProfileRepository).save(existing);
    }
}
