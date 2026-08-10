package com.example.DevConnect.event;

/**
 * Published when a recruiter changes an application's status. Only the id travels: the mail
 * service reloads the row after commit so it always mails the committed state.
 */
public record ApplicationStatusChangedEvent(Long applicationId) {
}
