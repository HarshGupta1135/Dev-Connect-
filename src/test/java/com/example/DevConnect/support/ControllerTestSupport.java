package com.example.DevConnect.support;

import com.example.DevConnect.config.SpringSecurity;
import com.example.DevConnect.oauth2.GitHubEmailAwareUserService;
import com.example.DevConnect.oauth2.OAuth2LoginHandler;
import com.example.DevConnect.oauth2.OAuth2ProviderRegistry;
import com.example.DevConnect.service.UserDetailsServiceImpl;
import com.example.DevConnect.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

/**
 * Base for @WebMvcTest controller tests.
 *
 * Loads the real security configuration and JWT filter, so every test goes through
 * the same URL rules, @PreAuthorize checks and exception handler as production.
 * No database, Redis or mail server is needed; each test mocks the services its
 * controller calls. Social login is configured off, as it is with no client ids.
 */
@Import({SpringSecurity.class, JwtUtil.class, ControllerTestSupport.SecurityBeans.class})
@TestPropertySource(properties = "jwt.secret=" + TestData.JWT_SECRET)
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JwtUtil jwtUtil;

    @MockitoBean
    protected UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private OAuth2LoginHandler oauth2LoginHandler;

    @MockitoBean
    private GitHubEmailAwareUserService gitHubEmailAwareUserService;

    @TestConfiguration
    static class SecurityBeans {
        @Bean
        OAuth2ProviderRegistry clientRegistrationRepository() {
            return new OAuth2ProviderRegistry(List.of());
        }
    }
}
