package com.example.DevConnect.event;

/**
 * Published when a developer's application has been persisted. Carries plain values rather
 * than entities so the listener never touches a detached entity outside a transaction.
 */
public record ApplicationSubmittedEvent(
        String developerEmail,
        String developerName,
        String jobTitle,
        String companyName
) {
}
