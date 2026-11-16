package com.example.DevConnect.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Locale;
import java.util.Set;

/**
 * Starts the provider sign-in exactly as Spring does, and also remembers the role
 * picked on the register page.
 *
 * The browser arrives at /oauth2/authorization/google?role=DEVELOPER. The role cannot
 * travel through Google or GitHub, so it is parked in the session that already holds
 * the OAuth handshake state, and read back once the provider sends the user back.
 * It only matters for a brand-new account; an existing account keeps its role.
 */
public class RoleCapturingAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    public static final String SIGNUP_ROLE_ATTRIBUTE = "devconnect.oauth2.signupRole";

    private static final Set<String> SIGNUP_ROLES = Set.of("DEVELOPER", "RECRUITER");

    private final OAuth2AuthorizationRequestResolver delegate;

    public RoleCapturingAuthorizationRequestResolver(ClientRegistrationRepository registrations) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                registrations, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return rememberRole(request, delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return rememberRole(request, delegate.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest rememberRole(HttpServletRequest request,
                                                    OAuth2AuthorizationRequest authorizationRequest) {
        // Null for every request that is not a sign-in start; touch nothing then.
        if (authorizationRequest == null) {
            return null;
        }

        String role = request.getParameter("role");
        role = role == null ? null : role.trim().toUpperCase(Locale.ROOT);

        // Only the two self-service roles. ADMIN must never be obtainable from a URL.
        if (role != null && SIGNUP_ROLES.contains(role)) {
            request.getSession().setAttribute(SIGNUP_ROLE_ATTRIBUTE, role);
        } else {
            request.getSession().removeAttribute(SIGNUP_ROLE_ATTRIBUTE);
        }
        return authorizationRequest;
    }
}
