package com.example.DevConnect.oauth2;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the GitHub profile and adds the account's verified primary email to it.
 *
 * GitHub's /user response has email = null for anyone who keeps their address
 * private, and when it is present it says nothing about whether it was verified.
 * /user/emails answers both, so that is the address an account is matched on.
 * (Google needs none of this: its ID token carries email and email_verified.)
 */
@Component
public class GitHubEmailAwareUserService extends DefaultOAuth2UserService {

    /** Attribute holding the verified primary address. Only ever set for GitHub. */
    public static final String VERIFIED_EMAIL_ATTRIBUTE = "devconnect_verified_email";

    private final RestClient github = RestClient.create("https://api.github.com");

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);

        if (!"github".equals(userRequest.getClientRegistration().getRegistrationId())) {
            return user;
        }

        Map<String, Object> attributes = new HashMap<>(user.getAttributes());
        String email = fetchVerifiedPrimaryEmail(userRequest.getAccessToken().getTokenValue());
        if (email != null) {
            attributes.put(VERIFIED_EMAIL_ATTRIBUTE, email);
        }

        String nameAttribute = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        return new DefaultOAuth2User(user.getAuthorities(), attributes, nameAttribute);
    }

    private String fetchVerifiedPrimaryEmail(String accessToken) {
        List<Map<String, Object>> emails;
        try {
            emails = github.get()
                    .uri("/user/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException ex) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("github_email_unavailable", "Could not read GitHub email addresses", null), ex);
        }

        if (emails == null) {
            return null;
        }
        return emails.stream()
                .filter(e -> Boolean.TRUE.equals(e.get("primary")) && Boolean.TRUE.equals(e.get("verified")))
                .map(e -> (String) e.get("email"))
                .findFirst()
                .orElse(null);
    }
}
