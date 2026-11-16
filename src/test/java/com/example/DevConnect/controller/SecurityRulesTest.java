package com.example.DevConnect.controller;

import com.example.DevConnect.dto.response.AccountResponse;
import com.example.DevConnect.service.AdminService;
import com.example.DevConnect.service.JobPostingService;
import com.example.DevConnect.service.UserPrincipal;
import com.example.DevConnect.service.UserService;
import com.example.DevConnect.support.ControllerTestSupport;
import com.example.DevConnect.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The URL-level rules in SpringSecurity and the JWT filter, end to end through
 * real requests: what is public, what needs a token, and what a bad token gets.
 */
@WebMvcTest(controllers = {HealthCheck.class, UserController.class, JobPostingController.class, AdminController.class})
class SecurityRulesTest extends ControllerTestSupport {

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private JobPostingService jobPostingService;
    @MockitoBean
    private AdminService adminService;

    private String bearer(String email, String... roles) {
        UserPrincipal principal = new UserPrincipal(TestData.user(1L, email, roles));
        when(userDetailsService.loadUserByUsername(email)).thenReturn(principal);
        return "Bearer " + jwtUtil.generateToken(principal);
    }

    @Test
    void healthCheckIsPublic() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void jobListingIsPublic() throws Exception {
        mvc.perform(get("/api/jobs")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointWithoutATokenIs403NotARedirect() throws Exception {
        mvc.perform(get("/api/account/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void validJwtAuthenticatesTheRequest() throws Exception {
        String header = bearer("dev@gmail.com", "DEVELOPER");
        when(userService.getAccount("dev@gmail.com"))
                .thenReturn(AccountResponse.builder().email("dev@gmail.com").role(List.of("DEVELOPER")).build());

        mvc.perform(get("/api/account/me").header("Authorization", header))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("dev@gmail.com"));
    }

    @Test
    void malformedJwtIsTreatedAsAnonymousNotAServerError() throws Exception {
        mvc.perform(get("/api/account/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isForbidden());
    }

    @Test
    void jwtForAUserThatNoLongerExistsIsRejected() throws Exception {
        String header = bearer("gone@gmail.com", "DEVELOPER");
        when(userDetailsService.loadUserByUsername("gone@gmail.com")).thenThrow(new UsernameNotFoundException("gone"));

        mvc.perform(get("/api/account/me").header("Authorization", header))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAreaRejectsNonAdmins() throws Exception {
        mvc.perform(get("/admin/get-all-users").header("Authorization", bearer("dev@gmail.com", "DEVELOPER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAreaAllowsAdmins() throws Exception {
        when(adminService.getAllUsers()).thenReturn(List.of());

        mvc.perform(get("/admin/get-all-users").header("Authorization", bearer("admin@gmail.com", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "dev@gmail.com", roles = "DEVELOPER")
    void wrongRoleOnAMethodSecuredEndpointGetsA403WithAMessage() throws Exception {
        mvc.perform(post("/api/recruiter/jobs").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You are not authorized to perform this action"));
    }

    @Test
    void socialLoginEndpointsAreClosedWhenNoProviderIsConfigured() throws Exception {
        mvc.perform(get("/oauth2/authorization/google")).andExpect(status().isForbidden());
    }
}
