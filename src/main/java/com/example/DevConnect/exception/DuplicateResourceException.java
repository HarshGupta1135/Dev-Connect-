package com.example.DevConnect.exception;

/**
 * The resource being created already exists (username, email, profile, skill).
 * Maps to HTTP 409.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
