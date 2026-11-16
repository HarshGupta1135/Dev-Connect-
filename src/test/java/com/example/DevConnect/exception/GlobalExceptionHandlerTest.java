package com.example.DevConnect.exception;

import com.example.DevConnect.dto.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Mapping of unique-constraint violations. The other handlers are exercised
 * through real requests in the controller tests.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static DataIntegrityViolationException violation(String dbMessage) {
        return new DataIntegrityViolationException("could not execute statement",
                new SQLIntegrityConstraintViolationException(dbMessage));
    }

    @Test
    void duplicateUsernameIsAFriendlyConflict() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolation(
                violation("Duplicate entry 'harsh' for key 'users.uk_users_user_name'"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("That username is already taken", response.getBody().getMessage());
        assertFalse(response.getBody().isSuccess());
    }

    @Test
    void duplicateEmailIsAFriendlyConflict() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolation(
                violation("Duplicate entry 'a@gmail.com' for key 'users.uk_users_email'"));

        assertEquals("That email is already registered to another account", response.getBody().getMessage());
    }

    @Test
    void otherConstraintGetsAGenericConflictMessage() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolation(
                violation("Duplicate entry 'Java' for key 'skills.name'"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("That value is already in use. Please choose another.", response.getBody().getMessage());
    }

    @Test
    void unexpectedErrorsAre500WithTheReason() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleGlobalException(new IllegalStateException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred: boom", response.getBody().getMessage());
    }
}
