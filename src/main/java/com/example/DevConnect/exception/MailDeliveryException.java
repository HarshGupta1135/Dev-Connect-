package com.example.DevConnect.exception;

/**
 * Raised when an email could not be handed to the mail server. Callers use it to decide
 * whether a notification may be marked as sent.
 */
public class MailDeliveryException extends RuntimeException {

    public MailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
