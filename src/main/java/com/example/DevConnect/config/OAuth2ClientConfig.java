package com.example.DevConnect.config;

import com.example.DevConnect.oauth2.OAuth2ProviderRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers Google and GitHub as sign-in providers, each only when its client id is
 * set (GOOGLE_CLIENT_ID / GITHUB_CLIENT_ID). See app.oauth2 in application.yaml.
 */
@Configuration
public class OAuth2ClientConfig {

    @Bean
    public OAuth2ProviderRegistry clientRegistrationRepository(
            @Value("${app.oauth2.backend-url:}") String backendUrl,
            @Value("${app.oauth2.google.client-id:}") String googleClientId,
            @Value("${app.oauth2.google.client-secret:}") String googleClientSecret,
            @Value("${app.oauth2.github.client-id:}") String githubClientId,
            @Value("${app.oauth2.github.client-secret:}") String githubClientSecret) {

        // Must match, character for character, the callback URL registered with each
        // provider: <backend>/login/oauth2/code/google and .../github.
        String redirectUri = backendUrl.isBlank()
                ? "{baseUrl}/login/oauth2/code/{registrationId}"
                : backendUrl.replaceAll("/+$", "") + "/login/oauth2/code/{registrationId}";

        List<ClientRegistration> registrations = new ArrayList<>();

        if (!googleClientId.isBlank()) {
            // openid + email + profile: the ID token carries email and email_verified.
            registrations.add(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(googleClientId)
                    .clientSecret(googleClientSecret)
                    .redirectUri(redirectUri)
                    .build());
        }

        if (!githubClientId.isBlank()) {
            // user:email is what lets us read the account's verified addresses; the
            // profile's own email field is null whenever the user keeps it private.
            registrations.add(CommonOAuth2Provider.GITHUB.getBuilder("github")
                    .clientId(githubClientId)
                    .clientSecret(githubClientSecret)
                    .scope("read:user", "user:email")
                    .redirectUri(redirectUri)
                    .build());
        }

        return new OAuth2ProviderRegistry(registrations);
    }
}
