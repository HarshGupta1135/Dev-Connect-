package com.example.DevConnect.oauth2;

import com.example.DevConnect.config.OAuth2ClientConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Which providers are switched on, and how the sign-in start captures the chosen role. */
class OAuth2ConfigurationTest {

    private static OAuth2ProviderRegistry registry(String backendUrl, String googleId, String githubId) {
        return new OAuth2ClientConfig().clientRegistrationRepository(
                backendUrl, googleId, "google-secret", githubId, "github-secret");
    }

    @Nested
    class ProviderRegistration {

        @Test
        void noClientIdsMeansSocialLoginIsOff() {
            OAuth2ProviderRegistry providers = registry("", "", "");

            assertFalse(providers.isEnabled());
            assertNull(providers.findByRegistrationId("google"));
        }

        @Test
        void onlyProvidersWithAClientIdAreRegistered() {
            OAuth2ProviderRegistry providers = registry("", "google-id", "");

            assertTrue(providers.isEnabled());
            assertNotNull(providers.findByRegistrationId("google"));
            assertNull(providers.findByRegistrationId("github"));
        }

        @Test
        void googleAsksForOpenIdEmailAndProfile() {
            ClientRegistration google = registry("", "google-id", "").findByRegistrationId("google");

            assertEquals("google-id", google.getClientId());
            assertTrue(google.getScopes().containsAll(List.of("openid", "email", "profile")));
        }

        @Test
        void githubAsksForAccessToEmailAddresses() {
            ClientRegistration github = registry("", "", "github-id").findByRegistrationId("github");

            assertTrue(github.getScopes().contains("user:email"));
        }

        @Test
        void callbackUrlIsWorkedOutFromTheRequestWhenNoBackendUrlIsSet() {
            ClientRegistration google = registry("", "google-id", "").findByRegistrationId("google");

            assertEquals("{baseUrl}/login/oauth2/code/{registrationId}", google.getRedirectUri());
        }

        @Test
        void configuredBackendUrlFixesTheCallbackUrl() {
            ClientRegistration google = registry("https://api.example.com/", "google-id", "").findByRegistrationId("google");

            assertEquals("https://api.example.com/login/oauth2/code/{registrationId}", google.getRedirectUri());
        }

        @Test
        void registryIsIterable() {
            List<String> ids = new ArrayList<>();
            registry("", "g", "h").forEach(r -> ids.add(r.getRegistrationId()));

            assertEquals(List.of("google", "github"), ids);
        }
    }

    @Nested
    class RoleCapture {

        private final RoleCapturingAuthorizationRequestResolver resolver =
                new RoleCapturingAuthorizationRequestResolver(registry("", "google-id", ""));

        private MockHttpServletRequest start(String role) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
            request.setServletPath("/oauth2/authorization/google");
            if (role != null) {
                request.setParameter("role", role);
            }
            return request;
        }

        private static Object storedRole(MockHttpServletRequest request) {
            return request.getSession().getAttribute(RoleCapturingAuthorizationRequestResolver.SIGNUP_ROLE_ATTRIBUTE);
        }

        @Test
        void chosenRoleIsRememberedUppercased() {
            MockHttpServletRequest request = start("recruiter");

            OAuth2AuthorizationRequest authorization = resolver.resolve(request);

            assertNotNull(authorization);
            assertTrue(authorization.getAuthorizationRequestUri().startsWith("https://accounts.google.com/"));
            assertEquals("RECRUITER", storedRole(request));
        }

        @Test
        void adminCanNeverBeRequested() {
            MockHttpServletRequest request = start("ADMIN");

            resolver.resolve(request);

            assertNull(storedRole(request));
        }

        @Test
        void startingWithoutARoleClearsAnEarlierChoice() {
            MockHttpServletRequest request = start(null);
            request.getSession().setAttribute(RoleCapturingAuthorizationRequestResolver.SIGNUP_ROLE_ATTRIBUTE, "DEVELOPER");

            resolver.resolve(request);

            assertNull(storedRole(request));
        }

        @Test
        void otherRequestsAreLeftAloneAndGetNoSession() {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/jobs");
            request.setServletPath("/api/jobs");
            request.setParameter("role", "DEVELOPER");

            assertNull(resolver.resolve(request));
            assertNull(request.getSession(false));
        }
    }
}
