package com.example.DevConnect.exception;

/**
 * The request clashes with something that already exists — a taken username, an
 * email already registered. Answered as 409, not 500: the caller can fix it.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
