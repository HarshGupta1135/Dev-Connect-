package com.example.DevConnect.exception;

/**
 * A rule about the request itself was broken (missing field, illegal state transition,
 * applying to a closed job, ...). Maps to HTTP 400.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
