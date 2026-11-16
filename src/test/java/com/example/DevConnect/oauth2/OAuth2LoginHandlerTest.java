package com.example.DevConnect.oauth2;

import com.example.DevConnect.oauth2.OAuth2AccountService.OAuth2SignInException;
import com.example.DevConnect.support.TestData;
import com.example.DevConnect.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginHandlerTest {

    private static final String CALLBACK = "https://app.example/oauth2/callback";

    @Mock
    private OAuth2AccountService accountService;

    private OAuth2LoginHandler handler;
    private JwtUtil jwtUtil;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", TestData.JWT_SECRET);
        handler = new OAuth2LoginHandler();
        ReflectionTestUtils.setField(handler, "frontendUrl", "https://app.example/");
        ReflectionTestUtils.setField(handler, "accountService", accountService);
        ReflectionTestUtils.setField(handler, "jwtUtil", jwtUtil);

        session = new MockHttpSession();
        request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        request.setSession(session);
        response = new MockHttpServletResponse();
    }

    private static OAuth2AuthenticationToken token(String provider, Map<String, Object> attributes) {
        String nameKey = "github".equals(provider) ? "id" : "sub";
        Map<String, Object> attrs = new HashMap<>(attributes);
        attrs.putIfAbsent(nameKey, "12345");
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("OAUTH2_USER")), attrs, nameKey);
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), provider);
    }

    private static Map<String, String> fragment(String redirect) {
        Map<String, String> values = new HashMap<>();
        for (String pair : redirect.substring(redirect.indexOf('#') + 1).split("&")) {
            String[] kv = pair.split("=", 2);
            values.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return values;
    }

    @Test
    void verifiedGoogleSignInRedirectsWithAJwtForThatAccount() throws Exception {
        session.setAttribute(RoleCapturingAuthorizationRequestResolver.SIGNUP_ROLE_ATTRIBUTE, "RECRUITER");
        when(accountService.findOrCreate("harsh@gmail.com", "Harsh Gupta", "RECRUITER"))
                .thenReturn(TestData.user(1L, "harsh@gmail.com", "RECRUITER"));

        handler.onAuthenticationSuccess(request, response, token("google",
                Map.of("email", "harsh@gmail.com", "email_verified", true, "name", "Harsh Gupta")));

        String redirect = response.getRedirectedUrl();
        assertTrue(redirect.startsWith(CALLBACK + "#token="), redirect);
        Map<String, String> values = fragment(redirect);
        assertEquals("harsh@gmail.com", jwtUtil.extractUsername(values.get("token")));
        assertEquals("harsh@gmail.com", values.get("email"));
        assertEquals("RECRUITER", values.get("role"));
        assertTrue(session.isInvalid(), "the handshake session is discarded");
    }

    @Test
    void unverifiedGoogleEmailIsRefused() throws Exception {
        handler.onAuthenticationSuccess(request, response, token("google",
                Map.of("email", "harsh@gmail.com", "email_verified", false)));

        assertEquals(CALLBACK + "?error=email_unverified", response.getRedirectedUrl());
        verify(accountService, never()).findOrCreate(any(), any(), any());
    }

    @Test
    void githubUsesTheVerifiedPrimaryEmailAndLogin() throws Exception {
        when(accountService.findOrCreate("octo@users.example", "octocat", null))
                .thenReturn(TestData.user(2L, "octo@users.example", "DEVELOPER"));

        handler.onAuthenticationSuccess(request, response, token("github", Map.of(
                "login", "octocat",
                "email", "public-but-unverified@example.com",
                GitHubEmailAwareUserService.VERIFIED_EMAIL_ATTRIBUTE, "octo@users.example")));

        assertEquals("octo@users.example", fragment(response.getRedirectedUrl()).get("email"));
    }

    @Test
    void githubWithoutAVerifiedEmailIsRefused() throws Exception {
        handler.onAuthenticationSuccess(request, response, token("github",
                Map.of("login", "octocat", "email", "unverified@example.com")));

        assertEquals(CALLBACK + "?error=email_unverified", response.getRedirectedUrl());
    }

    @Test
    void newUserWithoutARoleIsSentBackWithRoleRequired() throws Exception {
        when(accountService.findOrCreate(any(), any(), any())).thenThrow(new OAuth2SignInException("role_required"));

        handler.onAuthenticationSuccess(request, response, token("google",
                Map.of("email", "new@gmail.com", "email_verified", true)));

        assertEquals(CALLBACK + "?error=role_required", response.getRedirectedUrl());
    }

    @Test
    void unexpectedErrorsBecomeAGenericFailureNotA500() throws Exception {
        when(accountService.findOrCreate(any(), any(), any())).thenThrow(new IllegalStateException("db down"));

        handler.onAuthenticationSuccess(request, response, token("google",
                Map.of("email", "new@gmail.com", "email_verified", true)));

        assertEquals(CALLBACK + "?error=oauth_failed", response.getRedirectedUrl());
    }

    @Test
    void providerFailureRedirectsWithOauthFailed() throws Exception {
        handler.onAuthenticationFailure(request, response,
                new OAuth2AuthenticationException(new OAuth2Error("access_denied")));

        assertEquals(CALLBACK + "?error=oauth_failed", response.getRedirectedUrl());
        assertTrue(session.isInvalid());
    }
}
