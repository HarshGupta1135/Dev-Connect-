package com.example.DevConnect.controller;

import com.example.DevConnect.dto.request.JobPostingRequest;
import com.example.DevConnect.dto.response.CustomPageResponse;
import com.example.DevConnect.dto.response.JobPostingResponse;
import com.example.DevConnect.enums.JobType;
import com.example.DevConnect.exception.ResourceNotFoundException;
import com.example.DevConnect.exception.UnauthorizedException;
import com.example.DevConnect.service.JobPostingService;
import com.example.DevConnect.support.ControllerTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobPostingController.class)
class JobPostingControllerTest extends ControllerTestSupport {

    @MockitoBean
    private JobPostingService jobPostingService;

    private Pageable capturedPageable() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(jobPostingService).getActiveJobs(any(), any(), any(), pageable.capture(), any());
        return pageable.getValue();
    }

    @Test
    void listingDefaultsToNewestFirstPageOfTen() throws Exception {
        when(jobPostingService.getActiveJobs(any(), any(), any(), any(), any()))
                .thenReturn(CustomPageResponse.<JobPostingResponse>builder().content(List.of()).build());

        mvc.perform(get("/api/jobs")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));

        Pageable pageable = capturedPageable();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void listingPassesFiltersAndParsesSort() throws Exception {
        mvc.perform(get("/api/jobs")
                        .param("skills", "Java", "SQL")
                        .param("location", "Pune")
                        .param("type", "REMOTE")
                        .param("page", "2").param("size", "5")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk());

        verify(jobPostingService).getActiveJobs(eq(List.of("Java", "SQL")), eq("Pune"), eq(JobType.REMOTE), any(), isNull());
        Pageable pageable = capturedPageable();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
        assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("title").getDirection());
    }

    @Test
    void multipleSortFieldsAreSupported() throws Exception {
        mvc.perform(get("/api/jobs").param("sort", "title,asc").param("sort", "createdAt,desc"))
                .andExpect(status().isOk());

        Sort sort = capturedPageable().getSort();
        assertEquals(Sort.Direction.ASC, sort.getOrderFor("title").getDirection());
        assertEquals(Sort.Direction.DESC, sort.getOrderFor("createdAt").getDirection());
    }

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void signedInDeveloperGetsMatchScoring() throws Exception {
        mvc.perform(get("/api/jobs")).andExpect(status().isOk());

        verify(jobPostingService).getActiveJobs(any(), any(), any(), any(), eq("dev@gmail.com"));
    }

    @Test
    @WithMockUser(username = "rec@gmail.com", roles = "RECRUITER")
    void recruiterBrowsingJobsIsNotScored() throws Exception {
        mvc.perform(get("/api/jobs")).andExpect(status().isOk());

        verify(jobPostingService).getActiveJobs(any(), any(), any(), any(), isNull());
    }

    @Test
    void unknownJobTypeInTheQueryIsA400NotA500() throws Exception {
        mvc.perform(get("/api/jobs").param("type", "SPACE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid value 'SPACE' for 'type'"));
    }

    @Test
    void nonNumericJobIdIsA400NotA500() throws Exception {
        mvc.perform(get("/api/jobs/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for 'id'"));
    }

    @Test
    void nonNumericPageIsA400() throws Exception {
        mvc.perform(get("/api/jobs").param("page", "first")).andExpect(status().isBadRequest());
    }

    @Test
    void singleJobIsPublic() throws Exception {
        when(jobPostingService.getJobById(5L)).thenReturn(JobPostingResponse.builder().id(5L).title("Backend").build());

        mvc.perform(get("/api/jobs/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Backend"));
    }

    @Test
    void unknownJobIs404() throws Exception {
        when(jobPostingService.getJobById(404L)).thenThrow(new ResourceNotFoundException("Job Posting Not Found with ID: 404"));

        mvc.perform(get("/api/jobs/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job Posting Not Found with ID: 404"));
    }

    @Test
    @WithMockUser(username = "rec@gmail.com", roles = "RECRUITER")
    void recruiterCreatesAJob() throws Exception {
        mvc.perform(post("/api/recruiter/jobs").contentType("application/json")
                        .content("{\"title\":\"Backend\",\"jobType\":\"HYBRID\",\"requiredSkills\":[\"Java\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Job Created Successfully"));

        ArgumentCaptor<JobPostingRequest> request = ArgumentCaptor.forClass(JobPostingRequest.class);
        verify(jobPostingService).createJob(eq("rec@gmail.com"), request.capture());
        assertEquals("Backend", request.getValue().getTitle());
        assertEquals(JobType.HYBRID, request.getValue().getJobType());
    }

    @Test
    @WithMockUser(username = "rec@gmail.com", roles = "RECRUITER")
    void unknownJobTypeIsA400() throws Exception {
        mvc.perform(post("/api/recruiter/jobs").contentType("application/json").content("{\"jobType\":\"SPACE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "rec@gmail.com", roles = "RECRUITER")
    void editingSomeoneElsesJobIs403() throws Exception {
        doThrow(new UnauthorizedException("You are not authorized to update this job posting"))
                .when(jobPostingService).updateJob(eq("rec@gmail.com"), eq(5L), any());

        mvc.perform(put("/api/recruiter/jobs/5").contentType("application/json").content("{\"title\":\"x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not authorized to update this job posting"));
    }

    @Test
    @WithMockUser(username = "rec@gmail.com", roles = "RECRUITER")
    void recruiterClosesAJob() throws Exception {
        mvc.perform(patch("/api/recruiter/jobs/5/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Job Closed Successfully"));

        verify(jobPostingService).closeJobById("rec@gmail.com", 5L);
    }

    @Test
    @WithMockUser(username = "rec@gmail.com", roles = "RECRUITER")
    void recruiterListsTheirJobs() throws Exception {
        when(jobPostingService.getAllJobsByRecruiter("rec@gmail.com"))
                .thenReturn(List.of(JobPostingResponse.builder().id(1L).build()));

        mvc.perform(get("/api/recruiter/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void developerCannotUseRecruiterJobEndpoints() throws Exception {
        mvc.perform(get("/api/recruiter/jobs")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/recruiter/jobs/5/close")).andExpect(status().isForbidden());
    }
}
