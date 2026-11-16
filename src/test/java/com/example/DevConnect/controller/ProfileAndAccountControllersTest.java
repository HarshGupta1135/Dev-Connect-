package com.example.DevConnect.controller;

import com.example.DevConnect.dto.request.AccountUpdateRequest;
import com.example.DevConnect.dto.request.DeveloperProfileRequest;
import com.example.DevConnect.dto.request.RecruiterProfileRequest;
import com.example.DevConnect.dto.response.AccountResponse;
import com.example.DevConnect.dto.response.DeveloperProfileResponse;
import com.example.DevConnect.dto.response.RecruiterProfileResponse;
import com.example.DevConnect.entity.Skill;
import com.example.DevConnect.exception.ConflictException;
import com.example.DevConnect.exception.ResourceNotFoundException;
import com.example.DevConnect.service.AdminService;
import com.example.DevConnect.service.CloudinaryService;
import com.example.DevConnect.service.DeveloperProfileService;
import com.example.DevConnect.service.RecruiterProfileService;
import com.example.DevConnect.service.SkillService;
import com.example.DevConnect.service.UserService;
import com.example.DevConnect.support.ControllerTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {DeveloperProfileController.class, RecruiterProfileController.class,
        UserController.class, SkillController.class, AdminController.class})
class ProfileAndAccountControllersTest extends ControllerTestSupport {

    @MockitoBean
    private DeveloperProfileService developerProfileService;
    @MockitoBean
    private CloudinaryService cloudinaryService;
    @MockitoBean
    private RecruiterProfileService recruiterProfileService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private SkillService skillService;
    @MockitoBean
    private AdminService adminService;

    // ---------- developer profile ----------

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void developerCreatesProfile() throws Exception {
        when(developerProfileService.createProfile(any(), eq("dev@gmail.com"))).thenReturn("Profile Saved Successfully");

        mvc.perform(post("/api/developer/profile").contentType("application/json")
                        .content("{\"fullName\":\"Dev\",\"skills\":[\"Java\"],\"phone\":\"999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile Saved Successfully"));

        ArgumentCaptor<DeveloperProfileRequest> request = ArgumentCaptor.forClass(DeveloperProfileRequest.class);
        verify(developerProfileService).createProfile(request.capture(), eq("dev@gmail.com"));
        assertEquals(List.of("Java"), request.getValue().getSkills());
    }

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void developerReadsOwnProfile() throws Exception {
        when(developerProfileService.getProfile("dev@gmail.com"))
                .thenReturn(DeveloperProfileResponse.builder().fullName("Dev").build());

        mvc.perform(get("/api/developer/profile/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Dev"));
    }

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void missingProfileIs404() throws Exception {
        when(developerProfileService.getProfile("dev@gmail.com")).thenThrow(new ResourceNotFoundException("Profile Not Found"));

        mvc.perform(get("/api/developer/profile/me")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void resumeUploadStoresTheCloudinaryUrl() throws Exception {
        when(cloudinaryService.uploadResume(any())).thenReturn("https://cdn/resume.pdf");

        mvc.perform(multipart("/api/developer/profile/resume")
                        .file(new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[]{1})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumeUrl").value("https://cdn/resume.pdf"));

        verify(developerProfileService).updateResumeUrl("dev@gmail.com", "https://cdn/resume.pdf");
    }

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void emptyResumeIsRejected() throws Exception {
        mvc.perform(multipart("/api/developer/profile/resume")
                        .file(new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please select a file to upload."));
        verify(cloudinaryService, never()).uploadResume(any());
    }

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void uploadWithoutAFileIsA400NotA500() throws Exception {
        mvc.perform(multipart("/api/developer/profile/resume"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter 'file'"));
    }

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void uploadFailureIsReportedAsA400() throws Exception {
        when(cloudinaryService.uploadResume(any())).thenThrow(new RuntimeException("Failed to upload resume to Cloudinary"));

        mvc.perform(multipart("/api/developer/profile/resume")
                        .file(new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[]{1})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Failed to upload resume")));
    }

    @Test
    @WithMockUser(username = "rec@gmail.com", roles = "RECRUITER")
    void recruiterCannotEditADeveloperProfile() throws Exception {
        mvc.perform(put("/api/developer/profile").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    // ---------- recruiter profile ----------

    @Test
    @WithMockUser(username = "rec@gmail.com", roles = "RECRUITER")
    void recruiterProfileNeedsAFullName() throws Exception {
        mvc.perform(post("/api/recruiter/profile").contentType("application/json").content("{\"companyName\":\"Acme\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Full name is required"));
        verify(recruiterProfileService, never()).createProfile(any(), any());
    }

    @Test
    @WithMockUser(username = "rec@gmail.com", roles = "RECRUITER")
    void recruiterCreatesAndReadsProfile() throws Exception {
        when(recruiterProfileService.createProfile(any(RecruiterProfileRequest.class), eq("rec@gmail.com")))
                .thenReturn("Profile Created Successfully");
        when(recruiterProfileService.getProfile("rec@gmail.com"))
                .thenReturn(RecruiterProfileResponse.builder().companyName("Acme").build());

        mvc.perform(post("/api/recruiter/profile").contentType("application/json")
                        .content("{\"fullName\":\"Rec\",\"companyName\":\"Acme\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/recruiter/profile/me"))
                .andExpect(jsonPath("$.data.companyName").value("Acme"));
    }

    @Test
    @WithMockUser(username = "rec@gmail.com", roles = "RECRUITER")
    void recruiterViewsADeveloperProfile() throws Exception {
        when(recruiterProfileService.getDeveloperProfile("rec@gmail.com", 4L))
                .thenReturn(DeveloperProfileResponse.builder().fullName("Dev").build());

        mvc.perform(get("/api/developer/profile/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Dev"));
    }

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void developerCannotBrowseOtherDeveloperProfiles() throws Exception {
        mvc.perform(get("/api/developer/profile/4")).andExpect(status().isForbidden());
    }

    // ---------- account ----------

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void accountUpdateReturnsTheReplacementToken() throws Exception {
        when(userService.updateAccount(eq("dev@gmail.com"), any(AccountUpdateRequest.class)))
                .thenReturn(AccountResponse.builder().email("new@gmail.com").token("new-jwt").build());

        mvc.perform(put("/api/account").contentType("application/json")
                        .content("{\"userName\":\"dev\",\"email\":\"new@gmail.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("new-jwt"));
    }

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void accountUpdateValidatesTheBody() throws Exception {
        mvc.perform(put("/api/account").contentType("application/json")
                        .content("{\"userName\":\"ab\",\"email\":\"bad\",\"secondaryEmail\":\"also-bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Username must be between 3 and 40 characters")))
                .andExpect(jsonPath("$.message").value(containsString("Invalid email format")))
                .andExpect(jsonPath("$.message").value(containsString("Invalid secondary email format")));
    }

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void takenUsernameIsA409() throws Exception {
        when(userService.updateAccount(any(), any())).thenThrow(new ConflictException("That username is already taken"));

        mvc.perform(put("/api/account").contentType("application/json")
                        .content("{\"userName\":\"taken\",\"email\":\"dev@gmail.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("That username is already taken"));
    }

    // ---------- skills and admin ----------

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void anySignedInUserCanListSkills() throws Exception {
        when(skillService.getAllSkills()).thenReturn(List.of(new Skill(1L, "Java")));

        mvc.perform(get("/api/get/all/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Java"));
    }

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void onlyAdminsAddSkills() throws Exception {
        mvc.perform(post("/api/add/skills").contentType("application/json").content("[{\"name\":\"Go\"}]"))
                .andExpect(status().isForbidden());
        verify(skillService, never()).addSkills(any());
    }

    @Test
    @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
    void adminAddsSkills() throws Exception {
        when(skillService.addSkills(any())).thenReturn(List.of(new Skill(9L, "Go")));

        mvc.perform(post("/api/add/skills").contentType("application/json").content("[{\"name\":\"Go\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(9));
    }

    @Test
    @WithMockUser(username = "admin@gmail.com", roles = "ADMIN")
    void adminCreatesAnotherAdmin() throws Exception {
        doReturn(ResponseEntity.status(HttpStatus.CREATED).body("Admin Created Successfully"))
                .when(adminService).createAdmin(any());

        mvc.perform(post("/admin/add-admin").contentType("application/json")
                        .content("{\"userName\":\"root\",\"email\":\"root@gmail.com\",\"password\":\"x\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Admin Created Successfully"));
    }
}
