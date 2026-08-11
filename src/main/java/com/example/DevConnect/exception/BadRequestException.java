package com.example.DevConnect.exception;

/**
 * A rule the request breaks that bean validation cannot express on its own, because
 * it depends on the current state of the record being changed.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
