package com.example.DevConnect.controller;

import com.example.DevConnect.dto.request.ApplicationRequest;
import com.example.DevConnect.dto.response.ApplicantSummary;
import com.example.DevConnect.dto.response.ApplicationResponse;
import com.example.DevConnect.enums.ApplicationStatus;
import com.example.DevConnect.exception.BadRequestException;
import com.example.DevConnect.exception.DuplicateApplicationException;
import com.example.DevConnect.exception.UnauthorizedException;
import com.example.DevConnect.service.ApplicationService;
import com.example.DevConnect.support.ControllerTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
class ApplicationControllerTest extends ControllerTestSupport {

    @MockitoBean
    private ApplicationService applicationService;

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void developerApplies() throws Exception {
        mvc.perform(post("/api/developer/apply").contentType("application/json")
                        .content("{\"jobId\":7,\"coverNote\":\"Hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Application Submitted Successfully"));

        ArgumentCaptor<ApplicationRequest> request = ArgumentCaptor.forClass(ApplicationRequest.class);
        verify(applicationService).applyForJob(eq("dev@gmail.com"), request.capture());
        assertEquals(7L, request.getValue().getJobId());
        assertEquals("Hi", request.getValue().getCoverNote());
    }

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void incompleteProfileIsA400WithTheMissingFields() throws Exception {
        doThrow(new BadRequestException("Complete your profile before applying. Missing: resume"))
                .when(applicationService).applyForJob(any(), any());

        mvc.perform(post("/api/developer/apply").contentType("application/json").content("{\"jobId\":7}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Complete your profile before applying. Missing: resume"));
    }

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void applyingTwiceIsA409() throws Exception {
        doThrow(new DuplicateApplicationException("You have already applied for this job posting"))
                .when(applicationService).applyForJob(any(), any());

        mvc.perform(post("/api/developer/apply").contentType("application/json").content("{\"jobId\":7}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "rec@gmail.com", roles = "RECRUITER")
    void recruiterCannotApply() throws Exception {
        mvc.perform(post("/api/developer/apply").contentType("application/json").content("{\"jobId\":7}"))
                .andExpect(status().isForbidden());
        verify(applicationService, never()).applyForJob(any(), any());
    }

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void developerListsTheirApplications() throws Exception {
        when(applicationService.getJobApplications("dev@gmail.com")).thenReturn(List.of(
                ApplicationResponse.builder().id(1L).jobTitle("Backend").status(ApplicationStatus.APPLIED).build()));

        mvc.perform(get("/api/developer/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].jobTitle").value("Backend"))
                .andExpect(jsonPath("$.data[0].status").value("APPLIED"));
    }

    @Test
    @WithMockUser(username = "rec@gmail.com", roles = "RECRUITER")
    void recruiterSeesApplicantsForTheirJob() throws Exception {
        when(applicationService.getAllApplicantsById("rec@gmail.com", 7L)).thenReturn(List.of(
                ApplicationResponse.builder().id(1L)
                        .applicant(ApplicantSummary.builder().email("dev@gmail.com").skills(List.of("Java")).build())
                        .build()));

        mvc.perform(get("/api/recruiter/jobs/7/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].applicant.email").value("dev@gmail.com"));
    }

    @Test
    @WithMockUser(username = "rec@gmail.com", roles = "RECRUITER")
    void applicantsOfAnotherRecruitersJobAre403() throws Exception {
        when(applicationService.getAllApplicantsById("rec@gmail.com", 7L))
                .thenThrow(new UnauthorizedException("You are not authorized to view applications for this job posting"));

        mvc.perform(get("/api/recruiter/jobs/7/applications")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "rec@gmail.com", roles = "RECRUITER")
    void recruiterUpdatesStatus() throws Exception {
        mvc.perform(patch("/api/recruiter/applications/3/status").contentType("application/json")
                        .content("{\"newStatus\":\"SHORTLISTED\"}"))
                .andExpect(status().isOk());

        verify(applicationService).setApplicationStatus("rec@gmail.com", 3L, ApplicationStatus.SHORTLISTED);
    }

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void developerCannotChangeStatus() throws Exception {
        mvc.perform(patch("/api/recruiter/applications/3/status").contentType("application/json")
                        .content("{\"newStatus\":\"SHORTLISTED\"}"))
                .andExpect(status().isForbidden());
    }
}
