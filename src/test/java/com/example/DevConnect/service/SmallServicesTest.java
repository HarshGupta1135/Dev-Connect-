package com.example.DevConnect.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.example.DevConnect.entity.Skill;
import com.example.DevConnect.entity.User;
import com.example.DevConnect.enums.EmailPreference;
import com.example.DevConnect.repository.SkillRepository;
import com.example.DevConnect.repository.UserRepository;
import com.example.DevConnect.support.TestData;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The smaller services and the security user model. */
class SmallServicesTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class SkillServiceTest {

        @Mock
        private SkillRepository skillRepository;
        @InjectMocks
        private SkillService skillService;

        @Test
        void addsTrimmedNewSkills() {
            when(skillRepository.findByName(any())).thenReturn(Optional.empty());
            when(skillRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
            List<Skill> input = new ArrayList<>(List.of(new Skill(null, "  Java "), new Skill(null, "SQL")));

            List<Skill> saved = skillService.addSkills(input);

            assertEquals(List.of("Java", "SQL"), saved.stream().map(Skill::getName).toList());
        }

        @Test
        void emptyListIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> skillService.addSkills(List.of()));
            assertThrows(IllegalArgumentException.class, () -> skillService.addSkills(null));
        }

        @Test
        void blankNameIsRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> skillService.addSkills(List.of(new Skill(null, "  "))));
        }

        @Test
        void duplicateWithinRequestIsRejected() {
            when(skillRepository.findByName("Java")).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> skillService.addSkills(List.of(new Skill(null, "Java"), new Skill(null, " Java"))));
            assertTrue(ex.getMessage().contains("Duplicate skill name in request list: Java"));
            verify(skillRepository, never()).saveAll(any());
        }

        @Test
        void existingSkillIsRejected() {
            when(skillRepository.findByName("Java")).thenReturn(Optional.of(new Skill(1L, "Java")));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> skillService.addSkills(List.of(new Skill(null, "Java"))));
            assertTrue(ex.getMessage().contains("Skill already exists"));
        }

        @Test
        void listsAllSkills() {
            List<Skill> all = List.of(new Skill(1L, "Java"));
            when(skillRepository.findAll()).thenReturn(all);

            assertSame(all, skillService.getAllSkills());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class AdminServiceTest {

        @Mock
        private UserRepository userRepository;
        @Mock
        private PasswordEncoder passwordEncoder;
        @InjectMocks
        private AdminService adminService;

        @Test
        void createAdminEncodesPasswordAndGrantsAdminRole() {
            when(passwordEncoder.encode("pw")).thenReturn("ENCODED");
            User user = new User();
            user.setEmail("admin@gmail.com");
            user.setPassword("pw");

            ResponseEntity<?> response = adminService.createAdmin(user);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(saved.capture());
            assertEquals("ENCODED", saved.getValue().getPassword());
            assertEquals(List.of("ADMIN", "USER"), saved.getValue().getRole());
        }

        @Test
        void createAdminFailureIsReportedNotThrown() {
            when(passwordEncoder.encode(any())).thenReturn("ENCODED");
            when(userRepository.save(any())).thenThrow(new RuntimeException("db down"));
            User user = new User();
            user.setEmail("admin@gmail.com");
            user.setPassword("pw");

            ResponseEntity<?> response = adminService.createAdmin(user);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class CloudinaryServiceTest {

        @Mock
        private Cloudinary cloudinary;
        @InjectMocks
        private CloudinaryService cloudinaryService;

        @Test
        @SuppressWarnings({"unchecked", "rawtypes"})
        void uploadsAsRawFileInResumesFolderKeepingTheExtension() throws IOException {
            Uploader uploader = mock(Uploader.class);
            when(cloudinary.uploader()).thenReturn(uploader);
            when(uploader.upload(any(), anyMap())).thenReturn(Map.of("secure_url", "https://cdn/resumes/x.pdf"));

            String url = cloudinaryService.uploadResume(
                    new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[]{1, 2, 3}));

            assertEquals("https://cdn/resumes/x.pdf", url);
            ArgumentCaptor<Map> options = ArgumentCaptor.forClass(Map.class);
            verify(uploader).upload(any(), options.capture());
            assertEquals("resumes", options.getValue().get("folder"));
            assertEquals("raw", options.getValue().get("resource_type"));
            assertTrue(((String) options.getValue().get("public_id")).endsWith(".pdf"));
        }

        @Test
        void unreadableFileIsWrappedAsRuntimeException() throws IOException {
            MultipartFile file = mock(MultipartFile.class);
            when(cloudinary.uploader()).thenReturn(mock(Uploader.class));
            when(file.getOriginalFilename()).thenReturn("cv.pdf");
            when(file.getBytes()).thenThrow(new IOException("disk"));

            RuntimeException ex = assertThrows(RuntimeException.class, () -> cloudinaryService.uploadResume(file));
            assertEquals("Failed to upload resume to Cloudinary", ex.getMessage());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class UserDetailsServiceImplTest {

        @Mock
        private UserRepository userRepository;
        @InjectMocks
        private UserDetailsServiceImpl userDetailsService;

        @Test
        void loadsUserByEmail() {
            when(userRepository.findByEmail("dev@gmail.com"))
                    .thenReturn(Optional.of(TestData.user(1L, "dev@gmail.com", "DEVELOPER")));

            UserDetails details = userDetailsService.loadUserByUsername("dev@gmail.com");

            assertEquals("dev@gmail.com", details.getUsername());
        }

        @Test
        void unknownEmailIsUsernameNotFound() {
            when(userRepository.findByEmail("ghost@gmail.com")).thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("ghost@gmail.com"));
        }
    }

    @Nested
    class UserPrincipalTest {

        @Test
        void rolesBecomeSpringAuthoritiesAndEmailIsTheUsername() {
            UserPrincipal principal = new UserPrincipal(TestData.user(1L, "admin@gmail.com", "ADMIN", "USER"));

            assertEquals(List.of("ROLE_ADMIN", "ROLE_USER"),
                    principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
            assertEquals("admin@gmail.com", principal.getUsername());
            assertEquals("$2a$10$hash", principal.getPassword());
            assertTrue(principal.isEnabled());
        }
    }

    @Nested
    class UserNotificationEmailTest {

        @Test
        void defaultsToPrimary() {
            User user = TestData.user(1L, "dev@gmail.com", "DEVELOPER");
            assertEquals("dev@gmail.com", user.resolveNotificationEmail());
        }

        @Test
        void usesSecondaryWhenChosenAndPresent() {
            User user = TestData.user(1L, "dev@gmail.com", "DEVELOPER");
            user.setSecondaryEmail("alt@example.com");
            user.setEmailPreference(EmailPreference.SECONDARY);
            assertEquals("alt@example.com", user.resolveNotificationEmail());
        }

        @Test
        void fallsBackToPrimaryWhenSecondaryIsChosenButBlank() {
            User user = TestData.user(1L, "dev@gmail.com", "DEVELOPER");
            user.setSecondaryEmail("  ");
            user.setEmailPreference(EmailPreference.SECONDARY);
            assertEquals("dev@gmail.com", user.resolveNotificationEmail());
        }
    }
}
