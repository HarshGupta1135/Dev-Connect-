package com.example.DevConnect.oauth2;

import com.example.DevConnect.entity.User;
import com.example.DevConnect.oauth2.OAuth2AccountService.OAuth2SignInException;
import com.example.DevConnect.service.UserPrincipal;
import com.example.DevConnect.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Finishes a Google/GitHub sign-in by handing the frontend the same JWT that the
 * password login returns.
 *
 * The browser is redirected to {frontend}/oauth2/callback#token=...&email=...&role=...
 * The values ride in the fragment (after #), which browsers never send to any server,
 * so the token does not land in access logs. On failure it goes to
 * /oauth2/callback?error=<code> instead.
 */
@Component
public class OAuth2LoginHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginHandler.class);

    @Value("${app.oauth2.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Autowired
    private OAuth2AccountService accountService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String signupRole = takeSignupRoleAndEndSession(request);

        try {
            OAuth2AuthenticationToken oauth = (OAuth2AuthenticationToken) authentication;
            String provider = oauth.getAuthorizedClientRegistrationId();
            Map<String, Object> attributes = oauth.getPrincipal().getAttributes();

            String email = verifiedEmail(provider, attributes);
            if (email == null) {
                throw new OAuth2SignInException("email_unverified");
            }

            User user = accountService.findOrCreate(email, suggestedName(provider, attributes), signupRole);
            String token = jwtUtil.generateToken(new UserPrincipal(user));

            response.sendRedirect(callbackUrl()
                    + "#token=" + encode(token)
                    + "&email=" + encode(user.getEmail())
                    + "&role=" + encode(String.join(",", user.getRole())));

        } catch (OAuth2SignInException ex) {
            redirectWithError(response, ex.getCode());
        } catch (RuntimeException ex) {
            log.warn("OAuth sign-in could not be completed", ex);
            redirectWithError(response, "oauth_failed");
        }
    }

    /** The user cancelled at the provider, the state did not match, a token call failed... */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        takeSignupRoleAndEndSession(request);
        log.debug("OAuth sign-in failed: {}", exception.getMessage());
        redirectWithError(response, "oauth_failed");
    }

    /**
     * The session existed only to carry the OAuth handshake (state, the chosen role)
     * across the round trip to the provider. Everything after this is the JWT, so it
     * is dropped here rather than left to time out.
     */
    private String takeSignupRoleAndEndSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object role = session.getAttribute(RoleCapturingAuthorizationRequestResolver.SIGNUP_ROLE_ATTRIBUTE);
        session.invalidate();
        return role instanceof String r ? r : null;
    }

    private static String verifiedEmail(String provider, Map<String, Object> attributes) {
        if ("google".equals(provider)) {
            Object email = attributes.get("email");
            boolean verified = Boolean.TRUE.equals(attributes.get("email_verified"));
            return verified && email instanceof String e && !e.isBlank() ? e : null;
        }
        if ("github".equals(provider)) {
            Object email = attributes.get(GitHubEmailAwareUserService.VERIFIED_EMAIL_ATTRIBUTE);
            return email instanceof String e && !e.isBlank() ? e : null;
        }
        return null;
    }

    private static String suggestedName(String provider, Map<String, Object> attributes) {
        Object name = "github".equals(provider) ? attributes.get("login") : attributes.get("name");
        return name instanceof String s ? s : null;
    }

    private void redirectWithError(HttpServletResponse response, String code) throws IOException {
        response.sendRedirect(callbackUrl() + "?error=" + encode(code));
    }

    private String callbackUrl() {
        return frontendUrl.replaceAll("/+$", "") + "/oauth2/callback";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
