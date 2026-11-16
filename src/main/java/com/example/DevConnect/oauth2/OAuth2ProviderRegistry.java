package com.example.DevConnect.oauth2;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The OAuth providers that have credentials configured.
 *
 * Unlike Spring's InMemoryClientRegistrationRepository this may be empty, which is
 * the point: with no client ids set, social login is simply off and the security
 * configuration skips it, instead of the app refusing to start.
 */
public class OAuth2ProviderRegistry implements ClientRegistrationRepository, Iterable<ClientRegistration> {

    private final Map<String, ClientRegistration> registrations = new LinkedHashMap<>();

    public OAuth2ProviderRegistry(List<ClientRegistration> registrations) {
        registrations.forEach(r -> this.registrations.put(r.getRegistrationId(), r));
    }

    public boolean isEnabled() {
        return !registrations.isEmpty();
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return registrations.get(registrationId);
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        return Collections.unmodifiableCollection(registrations.values()).iterator();
    }
}
